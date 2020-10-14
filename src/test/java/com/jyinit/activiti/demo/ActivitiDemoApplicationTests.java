package com.jyinit.activiti.demo;

import lombok.extern.slf4j.Slf4j;
import org.activiti.engine.*;
import org.activiti.engine.history.HistoricActivityInstance;
import org.activiti.engine.history.HistoricActivityInstanceQuery;
import org.activiti.engine.repository.Deployment;
import org.activiti.engine.repository.ProcessDefinition;
import org.activiti.engine.repository.ProcessDefinitionQuery;
import org.activiti.engine.runtime.ProcessInstance;
import org.activiti.engine.task.Task;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;

@SpringBootTest
@Slf4j
class ActivitiDemoApplicationTests {

    @Test
    void contextLoads() {
    }

    @Autowired
    private ProcessEngine processEngine;

    /**
     * 流程定义的部署
     */
    @Test
    public void createDeploy() {
        RepositoryService repositoryService = processEngine.getRepositoryService();

        Deployment deployment = repositoryService.createDeployment()
                .addClasspathResource("holiday.bpmn")//添加bpmn资源
                //.addClasspathResource("diagram/holiday.png")
                .name("请假申请单流程")
                .deploy();

        log.info("流程部署id:" + deployment.getName());
        log.info("流程部署名称:" + deployment.getId());
    }

    /**
     * 启动流程实例:
     * 前提是先已经完成流程定义的部署工作
     * <p>
     * 背后影响的表：
     * act_hi_actinst     已完成的活动信息
     * act_hi_identitylink   参与者信息
     * act_hi_procinst   流程实例
     * act_hi_taskinst   任务实例
     * act_ru_execution   执行表
     * act_ru_identitylink   参与者信息
     * act_ru_task  任务
     */
    @Test
    public void startProcessInstance() {
        RuntimeService runtimeService = processEngine.getRuntimeService();
        //启动流程实例
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("holiday");

        log.info("流程定义ID:" + processInstance.getProcessDefinitionId());
        log.info("流程实例ID:" + processInstance.getId());
    }


    /**
     * 查询当前用户的任务列表
     */
    @Test
    public void findPersonalTaskList() {
        TaskService taskService = processEngine.getTaskService();

        //根据流程定义的key,负责人assignee来实现当前用户的任务列表查询
        List<Task> taskList = taskService.createTaskQuery()
                .processDefinitionKey("holiday")
                //.taskAssignee("张三")
                .list();

        for (Task task : taskList) {
            System.out.println("-----------------------");
            System.out.println("流程实例ID:" + task.getProcessInstanceId());
            System.out.println("任务ID:" + task.getId());
            System.out.println("任务负责人:" + task.getAssignee());
            System.out.println("任务名称:" + task.getName());
        }
    }

    /**
     * 处理当前用户的任务
     * 背后操作的表：
     * act_hi_actinst
     * act_hi_identitylink
     * act_hi_taskinst
     * act_ru_identitylink
     * act_ru_task
     */
    @Test
    public void completeTask() {
        String processDefinitionKey = "holiday";
        TaskService taskService = processEngine.getTaskService();

        Task task = taskService.createTaskQuery().processDefinitionKey(processDefinitionKey)
                //.taskAssignee("张三")
                .singleResult();
        if(task != null){
            //处理任务,结合当前用户任务列表的查询操作的话
            taskService.complete(task.getId());
            log.info("处理完成当前用户的任务");
        }else{
            log.info("当前用户暂无任务");
        }
    }

    @Test
    public void queryHistory() {
        HistoryService historyService = processEngine.getHistoryService();
        RepositoryService repositoryService = processEngine.getRepositoryService();
        //查询流程定义
        ProcessDefinitionQuery processDefinitionQuery = repositoryService.createProcessDefinitionQuery();
        String processDefinitionKey = "holiday";
        //遍历查询结果
        ProcessDefinition processDefinition = processDefinitionQuery.processDefinitionKey(processDefinitionKey)
                .orderByProcessDefinitionVersion().desc().singleResult();

        if (processDefinition != null) {
            HistoricActivityInstanceQuery query = historyService.createHistoricActivityInstanceQuery();

            List<HistoricActivityInstance> list = query.processDefinitionId(processDefinition.getId())
                    .orderByHistoricActivityInstanceStartTime().asc().list();//排序StartTime

            for (HistoricActivityInstance ai : list) {
                System.out.println(ai.getActivityId());
                System.out.println(ai.getActivityName());
                System.out.println(ai.getProcessDefinitionId());
                System.out.println(ai.getProcessInstanceId());
                System.out.println("==============================");
            }
        }
    }
}
