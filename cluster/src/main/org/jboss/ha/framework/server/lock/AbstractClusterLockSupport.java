/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2009, Red Hat Middleware LLC, and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */

package org.jboss.ha.framework.server.lock;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.Vector;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CopyOnWriteArrayList;

import org.jboss.ha.framework.interfaces.ClusterNode;
import org.jboss.ha.framework.interfaces.HAPartition;
import org.jboss.ha.framework.interfaces.HAPartition.HAMembershipListener;
import org.jboss.ha.framework.server.lock.ClusterLockState.State;
import org.jboss.logging.Logger;

/**
 * Base class for cluster-wide lock implementations.
 * 
 * @author Brian Stansberry
 * 
 * @version $Revision:$
 */
public abstract class AbstractClusterLockSupport implements HAMembershipListener
{
   public static final Class<?>[] REMOTE_LOCK_TYPES = new Class[]{Serializable.class, ClusterNode.class, long.class};
   public static final Class<?>[] RELEASE_REMOTE_LOCK_TYPES = new Class[]{Serializable.class, ClusterNode.class};
   
   /** 
    * Object the HAPartition can invoke on.  This class is static as
    * an aid in unit testing.
    */
   public static class RpcTarget
   {
      private final AbstractClusterLockSupport mgr;
      
      private RpcTarget(AbstractClusterLockSupport mgr)
      {
         this.mgr = mgr;
      }
      
      public RemoteLockResponse remoteLock(Serializable categoryName, ClusterNode caller, long timeout)
      {
         return mgr.remoteLock(categoryName, caller, timeout);
      }
      
      public void releaseRemoteLock(Serializable categoryName, ClusterNode caller)
      {
         mgr.releaseRemoteLock(categoryName, caller);
      }
   }
   
   protected final Logger log = Logger.getLogger(getClass());
   
   private final ConcurrentMap<Serializable, ClusterLockState> lockStates = new ConcurrentHashMap<Serializable, ClusterLockState>();
   private final ConcurrentMap<ClusterNode, Set<ClusterLockState>> lockStatesByOwner = new ConcurrentHashMap<ClusterNode, Set<ClusterLockState>>();
   private ClusterNode me;
   private final String serviceHAName;
   private final HAPartition partition;
   private final LocalLockHandler localHandler;
   private final List<ClusterNode> members = new CopyOnWriteArrayList<ClusterNode>();
//   private final boolean supportLocalOnly;
   private RpcTarget rpcTarget;
   
   public AbstractClusterLockSupport(String serviceHAName, 
                                 HAPartition partition, 
                                 LocalLockHandler handler)
   {
      if (serviceHAName == null)
      {
         throw new IllegalArgumentException("serviceHAName is null");         
      }
      if (partition == null)
      {
         throw new IllegalArgumentException("partition is null");         
      }
      if (handler == null)
      {
         throw new IllegalArgumentException("localHandler is null");
      }
      
      this.partition = partition;
      this.localHandler = handler;
      this.serviceHAName = serviceHAName;
   }
   
   // -------------------------------------------------------------- Properties
   
   public HAPartition getPartition()
   {
      return partition;
   }
   
   public String getServiceHAName()
   {
      return serviceHAName;
   }

   public LocalLockHandler getLocalHandler()
   {
      return localHandler;
   }
   
   // ------------------------------------------------------ ClusterLockManager
   
   public boolean lock(Serializable lockId, long timeout)
   {
      if (this.rpcTarget == null)
      {
         throw new IllegalStateException("Must call start() before first call to lock()");
      }
      ClusterLockState category = getClusterLockState(lockId, true);
      
      long left = timeout > 0 ? timeout : Long.MAX_VALUE;
      long start = System.currentTimeMillis();
      while (left > 0)
      {
         // Another node we lost to who should take precedence
         // over ourself in competition for the lock
         ClusterNode superiorCompetitor = null;
         
         // Only continue if category is unlocked
         if (category.state.compareAndSet(ClusterLockState.State.UNLOCKED, ClusterLockState.State.REMOTE_LOCKING))
         {            
            // Category state is now REMOTE_LOCKING, so other nodes will fail
            // in attempts to acquire on this node unless the caller is "superior"
            
            boolean success = false;
            try
            {
               // Get the lock on all other nodes in the cluster
               
               @SuppressWarnings("unchecked")
               ArrayList rsps = partition.callMethodOnCluster(getServiceHAName(), 
                     "remoteLock", new Object[]{lockId, me, new Long(left)}, 
                     REMOTE_LOCK_TYPES, true);
               
               boolean remoteLocked = true;
               if (rsps != null)
               {
                  for (Object rsp : rsps)
                  {
                     if ((rsp instanceof RemoteLockResponse) == false)
                     {
                        remoteLocked = false;
                     }
                     else if (((RemoteLockResponse) rsp).flag != RemoteLockResponse.Flag.OK)
                     {
                        RemoteLockResponse curRsp = (RemoteLockResponse) rsp;
                        remoteLocked = false;
                        if (superiorCompetitor == null)
                        {
                           superiorCompetitor = getSuperiorCompetitor(curRsp.holder);
                           log.debug("Received " + curRsp.flag + " response from " + 
                                 curRsp.responder + " -- reports lock is held by " + curRsp.holder);
                        }
                     }
                  }
               }
               else if ((members.size() == 1 && members.contains(me)) || members.size() == 0)
               {
                  // no peers
                  remoteLocked = true;
               }
               
               if (remoteLocked)
               {                  
                  // Bail if someone else locked our node while we were locking
                  // others.
                  if (category.state.compareAndSet(ClusterLockState.State.REMOTE_LOCKING, ClusterLockState.State.LOCAL_LOCKING))
                  {
                     // Now we are in LOCAL_LOCKING phase which will cause
                     // us to reject incoming remote requests
                     
                     // Now see if we can lock our own node
                     long localTimeout = left - (System.currentTimeMillis() - start);
                     if (getLock(lockId, category, me, localTimeout).flag == RemoteLockResponse.Flag.OK)
                     {         
                        success = true;
                        return true;
                     }
                     // else either 1) there was a race with a remote caller or 
                     // 2) some other activity locally is preventing our
                     // acquisition of the local lock. Either way back off
                     // and then retry
                  }
                  
                  // Find out if we couldn't acquire because someone with
                  // precedence has the lock
                  superiorCompetitor = getSuperiorCompetitor(category.getHolder());
               }
            }
            catch (RuntimeException e)
            {
               throw e;
            }
            catch (Exception e)
            {
               throw new RuntimeException(e);
            }
            finally
            {
               if (!success)
               {
                  cleanup(lockId, category);
               }
            }
         }      
         
         // We failed for some reason. Pause to let things clear before trying again. 
         // If we've detected we are competing with someone else who is 
         // "superior" pause longer to let them proceed first.
         long backoff = computeBackoff(timeout, start, left, superiorCompetitor == null);
         if (backoff > 0)
         {
            try
            {
               Thread.sleep(backoff);
            }
            catch (InterruptedException e)
            {
               Thread.currentThread().interrupt();
               break;
            }
         }
         
         if (category.state.get() == ClusterLockState.State.INVALID)
         {
            // Someone invalidated our category; get a new one
            category = getClusterLockState(lockId, true);
         }   
                  
         long now = System.currentTimeMillis();
         left -= (now - start);
         start = now;
      }
      
      return false;
   }
   
   public abstract void unlock(Serializable lockId);
   
   public String getPartitionName()
   {
      return partition.getPartitionName();
   }
   
   public ClusterNode getLocalClusterNode()
   {
      return this.me;
   }
   
   public List<ClusterNode> getCurrentView()
   {
      return new ArrayList<ClusterNode>(members);
   }
   
   public void start() throws Exception
   {
      this.me = this.partition.getClusterNode();
      this.localHandler.setLocalNode(this.me);
      
      this.rpcTarget = new RpcTarget(this);
      this.partition.registerRPCHandler(this.serviceHAName, this.rpcTarget);
      this.partition.registerMembershipListener(this); 
      
      Vector<ClusterNode> allMembers = new Vector<ClusterNode>();
      for (ClusterNode node : this.partition.getClusterNodes())
      {
         allMembers.add(node);
      }
      membershipChanged(new Vector<ClusterNode>(), allMembers, allMembers);
   }
   
   public void stop() throws Exception
   {
      if (this.rpcTarget != null)
      {
         this.partition.unregisterRPCHandler(this.serviceHAName, this.rpcTarget);
         this.rpcTarget = null;
         this.partition.unregisterMembershipListener(this);  
         Vector<ClusterNode> dead = new Vector<ClusterNode>(members);
         Vector<ClusterNode> empty = new Vector<ClusterNode>();
         membershipChanged(dead, empty, empty);
         this.me = null;
      }
   }
   
   // ---------------------------------------------------- HAMembershipListener

   @SuppressWarnings("unchecked")
   public synchronized void membershipChanged(Vector deadMembers, Vector newMembers, Vector allMembers)
   {
      this.members.clear();
      this.members.addAll(allMembers);
      
      Set<ClusterNode> toClean = lockStatesByOwner.keySet();
      toClean.removeAll(this.members);
      for (ClusterNode deadMember : toClean)
      {
         Set<ClusterLockState> deadMemberLocks = lockStatesByOwner.remove(deadMember);
         if (deadMemberLocks != null)
         {
            synchronized (deadMemberLocks)
            {
               // We're going to iterate and make a call that removes from set,
               // so iterate over a copy to avoid ConcurrentModificationException
               Set<ClusterLockState> copy = new HashSet<ClusterLockState>(deadMemberLocks);
               for (ClusterLockState lockState : copy)
               {
                  releaseRemoteLock(lockState.lockId, (ClusterNode) deadMember);               
               }
            }
         }
         
      }
   }
   
   // --------------------------------------------------------------- Protected

   protected abstract RemoteLockResponse handleLockSuccess(ClusterLockState lockState, ClusterNode caller);
   
   protected abstract ClusterLockState getClusterLockState(Serializable categoryName);
   
   protected abstract RemoteLockResponse yieldLock(ClusterLockState lockState, ClusterNode caller, long timeout);

   protected void recordLockHolder(ClusterLockState lockState, ClusterNode caller)
   {
      if (lockState.holder != null)
      {
         Set<ClusterLockState> memberLocks = getLocksHeldByMember(lockState.holder);
         synchronized (memberLocks)
         {
            memberLocks.remove(lockState);
         }
      }
      
      if (me.equals(caller) == false)
      {
         Set<ClusterLockState> memberLocks = getLocksHeldByMember(caller);
         synchronized (memberLocks)
         {
            memberLocks.add(lockState);
         }
      }
      
      lockState.lock(caller);
   }
   
   protected ClusterLockState getClusterLockState(Serializable lockName, boolean create)
   {
      ClusterLockState category = lockStates.get(lockName);
      if (category == null && create)
      {
         category = new ClusterLockState(lockName);
         ClusterLockState existing = lockStates.putIfAbsent(lockName, category);
         if (existing != null)
         {
            category = existing;
         }         
      }
      return category;
   }
   
   protected void removeLockState(ClusterLockState lockState)
   {
      lockStates.remove(lockState.lockId, lockState);
   }
   
   // ----------------------------------------------------------------- Private
   
   /**
    * Called by a remote node via RpcTarget.
    */
   private RemoteLockResponse remoteLock(Serializable lockName, ClusterNode caller, long timeout)
   {
      RemoteLockResponse response = null;
      ClusterLockState lockState = getClusterLockState(lockName);
      if (lockState == null)
      {
         // unknown == OK
         return new RemoteLockResponse(me, RemoteLockResponse.Flag.OK);
      }
      
      switch (lockState.state.get())
      {
         case UNLOCKED:
            // Remote callers can race for the local lock
            response = getLock(lockName, lockState, caller, timeout);
            break;
         case REMOTE_LOCKING:
            if (me.equals(caller))
            {
               log.warn("Received remoteLock call from self");
               response = new RemoteLockResponse(me, RemoteLockResponse.Flag.OK);
            }
            else if (getSuperiorCompetitor(caller) == null)
            {
               // I want the lock and I take precedence
               response = new RemoteLockResponse(me, RemoteLockResponse.Flag.REJECT, me);
            }
            else
            {
               // I want the lock but caller takes precedence; let
               // them acquire local lock and I'll fail
               response = getLock(lockName, lockState, caller, timeout);
            }
            break;
         case LOCAL_LOCKING:
            // I've gotten OK responses from everyone and am about
            // to acquire local lock, so reject caller
            response = new RemoteLockResponse(me, RemoteLockResponse.Flag.REJECT, me);
            break;
         case LOCKED:
            // See if I have the lock and will give it up
            response = yieldLock(lockState, caller, timeout);
            break;
         case INVALID:
            // We've given up the lock since we got the category and the
            // thread that caused that is trying to discard the unneeded 
            // category.
            // Call in again and see what our current state is
            Thread.yield();
            response = remoteLock(lockName, caller, timeout);
            break;
      }
      
      return response;
   }
   
   /**
    * Called by a remote node via RpcTarget.
    */
   private void releaseRemoteLock(Serializable categoryName, ClusterNode caller)
   {
      ClusterLockState lockState = getClusterLockState(categoryName, false);
      if (lockState != null && lockState.state.get() == State.LOCKED)
      {
         if (caller.equals(localHandler.getLockHolder(categoryName)))
         {
            // Throw away the category as a cleanup exercise
            lockState.invalidate();
            localHandler.unlockFromCluster(categoryName, caller);
            Set<ClusterLockState> memberLocks = getLocksHeldByMember(caller);
            synchronized (memberLocks)
            {
               memberLocks.remove(lockState);
            }
            removeLockState(lockState);
         }
      }
      
   }

   /** See if <code>caller</code> comes before us in the members list */
   private ClusterNode getSuperiorCompetitor(ClusterNode caller)
   {
      if (caller == null)
         return null;
      
      for (ClusterNode node : members)
      {
         if (me.equals(node))
         {
            break;
         }
         else if (caller.equals(node))
         {
            return caller;
         }
      }
      
      return null;
   }

   /**
    * Always call this with a lock on the Category.
    */
   protected RemoteLockResponse getLock(Serializable categoryName, ClusterLockState category, 
                            ClusterNode caller, long timeout)
   {
      RemoteLockResponse response;
      try
      {
         localHandler.lockFromCluster(categoryName, caller, timeout);
      }
      catch (InterruptedException e)
      {
         Thread.currentThread().interrupt();
         log.error("Caught InterruptedException; Failing request by " + caller + " to lock " + categoryName);
         return new RemoteLockResponse(me, RemoteLockResponse.Flag.FAIL, localHandler.getLockHolder(categoryName));
      }
      catch (TimeoutException t)
      {
         return new RemoteLockResponse(me, RemoteLockResponse.Flag.FAIL, t.getOwner());
      }
      
      response = handleLockSuccess(category, caller);
      return response;
   }
   
   private Set<ClusterLockState> getLocksHeldByMember(ClusterNode member)
   {
      Set<ClusterLockState> memberCategories = lockStatesByOwner.get(member);
      if (memberCategories == null)
      {
         memberCategories = new HashSet<ClusterLockState>();
         Set<ClusterLockState> existing = lockStatesByOwner.putIfAbsent(member, memberCategories);
         if (existing != null)
         {
            memberCategories = existing;
         }
      }
      return memberCategories;
   }

   /** Back out of a failed attempt by the local node to lock */
   private void cleanup(Serializable categoryName, ClusterLockState category)
   {
      try
      {
         partition.callMethodOnCluster(getServiceHAName(), "releaseRemoteLock", new Object[]{categoryName, me}, RELEASE_REMOTE_LOCK_TYPES, true);
      }
      catch (RuntimeException e)
      {
         throw e;
      }
      catch (Exception e)
      {
         throw new RuntimeException("Failed releasing remote lock", e);
      }
      finally
      {
         if (category.state.compareAndSet(ClusterLockState.State.REMOTE_LOCKING, ClusterLockState.State.UNLOCKED) == false)
         {
            category.state.compareAndSet(ClusterLockState.State.LOCAL_LOCKING, ClusterLockState.State.UNLOCKED);
         }
      }
   }
   
   private static long computeBackoff(long initialTimeout, long start, long left, boolean superiorCompetitor)
   {
      long remain = left - (System.currentTimeMillis() - start);
      // Don't spam the cluster
      if (remain < Math.min(initialTimeout / 5, 15))
      {
         return remain;
      }
      
      long max = superiorCompetitor ? 100 : 250;
      long min = remain / 3;
      return Math.min(max, min);
   }
}
