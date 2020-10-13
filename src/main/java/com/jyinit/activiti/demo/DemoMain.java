package com.jyinit.activiti.demo;

import lombok.extern.slf4j.Slf4j;
import org.activiti.engine.*;
import org.activiti.engine.form.FormProperty;
import org.activiti.engine.form.TaskFormData;
import org.activiti.engine.impl.form.DateFormType;
import org.activiti.engine.impl.form.StringFormType;
import org.activiti.engine.repository.Deployment;
import org.activiti.engine.repository.DeploymentBuilder;
import org.activiti.engine.repository.ProcessDefinition;
import org.activiti.engine.runtime.ProcessInstance;
import org.activiti.engine.task.Task;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Scanner;


/**
 * @author AJin ajin0369@outlook.com
 */
@Slf4j
public class DemoMain {
    public static void main(String[] args) {
        log.info("启动流程");
        //创建流程引擎
        ProcessEngine processEngine = getProcessEngine();

        //部署流程定义文件
        ProcessDefinition processDefinition = getProcessDefinition(processEngine);

        //启动运行流程
        ProcessInstance processInstance= startInstance(processEngine, processDefinition);

        //处理流程任务

        Scanner scanner = new Scanner(System.in);
        while(processInstance.isEnded()){
            TaskService taskService = processEngine.getTaskService();
            for (Task task : taskService.createTaskQuery().list()) {
                log.info("待处理任务：{}", task.getName());
                FormService formService = processEngine.getFormService();
                TaskFormData taskFormData = formService.getTaskFormData(task.getId());
                List<FormProperty> formProperties = taskFormData.getFormProperties();
                final HashMap<String, Object> variables = new HashMap<>();
                for (FormProperty formProperty : formProperties) {
                    String line = null;
                    if (formProperty.getType() instanceof StringFormType) {
                        log.info("请输入：{} ?", formProperty.getName());
                        line = scanner.nextLine();
                        variables.put(formProperty.getId(), line);
                    } else if (formProperty.getType() instanceof DateFormType) {
                        log.info("请输入 {} 格式 ? ( yyyy-MM-dd )", formProperty.getName());
                        line = scanner.nextLine();
                        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd");
                        try {
                            Date date = simpleDateFormat.parse(line);
                            variables.put(formProperty.getId(), date);
                        } catch (ParseException e) {
                            e.printStackTrace();
                        }
                    } else {
                        log.info("你输入的不支持");
                    }
                    log.info("您输入的内容是：{}", line);
                }
                taskService.complete(task.getId(), variables);
                processInstance = processEngine.getRuntimeService()
                        .createProcessInstanceQuery()
                        .processInstanceId(processInstance.getId()).singleResult();
            }

        }



    }

    private static ProcessInstance startInstance(ProcessEngine processEngine, ProcessDefinition processDefinition) {
        RuntimeService runtimeService = processEngine.getRuntimeService();
        ProcessInstance processInstance = runtimeService.startProcessInstanceById(processDefinition.getId());
        log.info("启动流程：{}", processInstance.getProcessDefinitionKey());
        return processInstance;
    }

    private static ProcessDefinition getProcessDefinition(ProcessEngine processEngine) {
        RepositoryService repositoryService = processEngine.getRepositoryService();
        DeploymentBuilder deploymentBuilder = repositoryService.createDeployment();
        deploymentBuilder.addClasspathResource("approve.bpmn20.xml");
        Deployment deploy = deploymentBuilder.deploy();
        String id = deploy.getId();
        ProcessDefinition processDefinition = repositoryService
                .createProcessDefinitionQuery()
                .deploymentId(id).singleResult();
        log.info("流程定义文件：{},流程id：{}",processDefinition.getName(),processDefinition.getId());
        return processDefinition;
    }

    private static ProcessEngine getProcessEngine() {
        ProcessEngineConfiguration cfg=ProcessEngineConfiguration.createStandaloneInMemProcessEngineConfiguration();
        ProcessEngine processEngine = cfg.buildProcessEngine();
        String name = processEngine.getName();
        log.info("流程引擎名称{}",name);
        return processEngine;
    }
}
