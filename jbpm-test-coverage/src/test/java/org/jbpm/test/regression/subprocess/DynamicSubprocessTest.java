/*
 * Copyright 2017 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jbpm.test.regression.subprocess;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.assertj.core.api.Assertions;
import org.jbpm.test.JbpmTestCase;
import org.junit.Test;
import org.kie.api.event.process.DefaultProcessEventListener;
import org.kie.api.event.process.ProcessCompletedEvent;
import org.kie.api.event.process.ProcessNodeTriggeredEvent;
import org.kie.api.event.process.ProcessStartedEvent;
import org.kie.api.runtime.KieSession;
import org.kie.api.task.TaskService;
import org.kie.api.task.model.Task;

public class DynamicSubprocessTest extends JbpmTestCase {

    private static final String DYNAMIC_PARENT_SUBPROCESS_PARENT = "org/jbpm/test/regression/subprocess/DynamicParentProcess.bpmn2";
    private static final String DYNAMIC_PARENT_SUBPROCESS_CHILD = "org/jbpm/test/regression/subprocess/DynamicParentSubProcess.bpmn2";
    private static final String DYNAMIC_PARENT_SUBPROCESS_ID = "org.jbpm.test.regression.subprocess.DynamicParentProcess";

    static {
        System.setProperty("jbpm.enable.multi.con", "true");
    }

    @Test
    public void testDuplicateProcess() throws Exception {

        addProcessEventListener(new DefaultProcessEventListener() {
            @Override
            public void afterNodeTriggered(ProcessNodeTriggeredEvent event) {
                System.out.println(event);
            }

            @Override
            public void afterProcessStarted(ProcessStartedEvent event) {
                System.out.println(event + " pid " + event.getProcessInstance().getId());
            }
            
            @Override
            public void afterProcessCompleted(ProcessCompletedEvent event) {
                System.out.println(event);
            }
        });

        createRuntimeManager(Strategy.PROCESS_INSTANCE, "DynamicSubprocessTest", 
                             DYNAMIC_PARENT_SUBPROCESS_PARENT,
                             DYNAMIC_PARENT_SUBPROCESS_CHILD);


        KieSession ksession = getRuntimeEngine().getKieSession();
        Assertions.assertThat(ksession).isNotNull();
        Map<String, Object> params = new HashMap<>();
        params.put("isLoopback", true);
        params.put("isBypass", true);
        params.put("counter", 1);
        Long pid = ksession.startProcess(DYNAMIC_PARENT_SUBPROCESS_ID, params).getId();

        TaskService taskService = getRuntimeEngine().getTaskService();
        List<Long> tasks = taskService.getTasksByProcessInstanceId(2);
        Task task = taskService.getTaskById(tasks.get(0));
        taskService.start(task.getId(), "john");
        taskService.complete(task.getId(), "john", Collections.emptyMap());
        assertProcessInstanceCompleted(pid);
    }

}
