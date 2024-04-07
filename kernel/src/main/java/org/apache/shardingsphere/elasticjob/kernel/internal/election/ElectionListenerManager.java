/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.shardingsphere.elasticjob.kernel.internal.election;

import org.apache.shardingsphere.elasticjob.kernel.internal.sharding.JobInstance;
import org.apache.shardingsphere.elasticjob.kernel.internal.listener.AbstractListenerManager;
import org.apache.shardingsphere.elasticjob.kernel.internal.schedule.JobRegistry;
import org.apache.shardingsphere.elasticjob.kernel.internal.server.ServerNode;
import org.apache.shardingsphere.elasticjob.kernel.internal.server.ServerService;
import org.apache.shardingsphere.elasticjob.kernel.internal.server.ServerStatus;
import org.apache.shardingsphere.elasticjob.reg.base.CoordinatorRegistryCenter;
import org.apache.shardingsphere.elasticjob.reg.listener.DataChangedEvent;
import org.apache.shardingsphere.elasticjob.reg.listener.DataChangedEvent.Type;
import org.apache.shardingsphere.elasticjob.reg.listener.DataChangedEventListener;

import java.util.Objects;

/**
 * Election listener manager.
 */
public final class ElectionListenerManager extends AbstractListenerManager {
    
    private final String jobName;
    
    private final LeaderNode leaderNode;
    
    private final ServerNode serverNode;
    
    private final LeaderService leaderService;
    
    private final ServerService serverService;
    
    public ElectionListenerManager(final CoordinatorRegistryCenter regCenter, final String jobName) {
        super(regCenter, jobName);
        this.jobName = jobName;
        leaderNode = new LeaderNode(jobName);
        serverNode = new ServerNode(jobName);
        leaderService = new LeaderService(regCenter, jobName);
        serverService = new ServerService(regCenter, jobName);
    }
    
    @Override
    public void start() {
        addDataListener(new LeaderElectionJobListener());
        addDataListener(new LeaderAbdicationJobListener());
    }

    /**
     * 主要是当主节点宕机后触发重新选主监听器。
     * 监听节点 `/{jobName}/leader/election/instance`
     */
    class LeaderElectionJobListener implements DataChangedEventListener {

        @Override
        public void onChange(final DataChangedEvent event) {
            // job 未停止 && 当前服务器不是主节点 || instance 节点被删除时
            if (!JobRegistry.getInstance().isShutdown(jobName)
                    && (isActiveElection(event.getKey(), event.getValue())
                    || isPassiveElection(event.getKey(), event.getType()))) {
                leaderService.electLeader();
            }
        }

        /**
         * leader节点不存在
         *   && 当前服务器运行正常(运行正常的依据是存在{jobName}/servers/serverip)
         *   && 节点内容不为DISABLED
         * @param path
         * @param data
         * @return
         */
        private boolean isActiveElection(final String path, final String data) {
            return !leaderService.hasLeader() && isLocalServerEnabled(path, data);
        }

        /**
         * 如果当前事件节点为 `/{jobName}/leader/election/instance` 并且事件类型为删除
         *  && 该job的当前对应的实例(/{jobName}/instances/ip)存在并且状态不为DISABLED。
         * @param path
         * @param eventType
         * @return
         */
        private boolean isPassiveElection(final String path, final Type eventType) {
            JobInstance jobInstance = JobRegistry.getInstance().getJobInstance(jobName);
            return !Objects.isNull(jobInstance)
                    && isLeaderCrashed(path, eventType)
                    && serverService.isAvailableServer(jobInstance.getServerIp());
        }

        private boolean isLeaderCrashed(final String path, final Type eventType) {
            return leaderNode.isLeaderInstancePath(path) && Type.DELETED == eventType;
        }

        private boolean isLocalServerEnabled(final String path, final String data) {
            return serverNode.isLocalServerPath(path) && !ServerStatus.DISABLED.name().equals(data);
        }
    }

    /**
     * 主节点退位监听器.
     * 当通过配置方式在线设置主节点状态为disabled时, 需要删除主节点信息从而再次激活选主事件.
     */
    class LeaderAbdicationJobListener implements DataChangedEventListener {

        @Override
        public void onChange(final DataChangedEvent event) {
            if (leaderService.isLeader() && isLocalServerDisabled(event.getKey(), event.getValue())) {
                leaderService.removeLeader();
            }
        }

        private boolean isLocalServerDisabled(final String path, final String data) {
            return serverNode.isLocalServerPath(path) && ServerStatus.DISABLED.name().equals(data);
        }
    }
}
