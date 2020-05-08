package com.example.demo;

import com.nedap.archie.adlparser.ADLParser;
import com.nedap.archie.aom.Archetype;
import com.nedap.archie.aom.OperationalTemplate;
import com.nedap.archie.creation.ExampleJsonInstanceGenerator;
import com.nedap.archie.flattener.Flattener;
import com.nedap.archie.flattener.InMemoryFullArchetypeRepository;
import org.junit.jupiter.api.Test;
import org.kie.api.KieServices;
import org.kie.api.runtime.KieContainer;
import org.kie.api.runtime.KieSession;
import org.openehr.referencemodels.BuiltinReferenceModels;
import org.springframework.boot.test.context.SpringBootTest;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Map;

@SpringBootTest
class DemoApplicationTests {

    @Test
    void contextLoads() throws IOException {
        String TYPE_PROPERTY_NAME = "_type";
        ADLParser parser = new ADLParser();
        Archetype archetype;
        try(InputStream stream =  getClass().getResourceAsStream("/openEHR-EHR-OBSERVATION.respiration.v1.0.0.adls")) {
            archetype = parser.parse(stream);
            if(parser.getErrors().hasErrors()) {
                throw new RuntimeException(parser.getErrors().toString());
            }
        }
        InMemoryFullArchetypeRepository repository = new InMemoryFullArchetypeRepository();
        repository.addArchetype(archetype);
        Flattener flattener = new Flattener(repository, BuiltinReferenceModels.getMetaModels()).createOperationalTemplate(true);
        OperationalTemplate opt = (OperationalTemplate) flattener.flatten(archetype);
        ExampleJsonInstanceGenerator structureGenerator = new ExampleJsonInstanceGenerator(BuiltinReferenceModels.getMetaModels(), "en");
        structureGenerator.setTypePropertyName(TYPE_PROPERTY_NAME);
        Map<String, Object> structure = structureGenerator.generate(opt);
        Map<String, Object> data = (Map<String, Object>) structure.get("data");

        List events = (List) data.get("events");
        Map<String, Object> event = (Map<String, Object>) events.get(0);
        Map<String, Object> itemtree = (Map<String, Object>) event.get("data");
        List items = (List) itemtree.get("items");
        Map<String, Object> item = (Map<String, Object>) items.get(0);
        Map<String, Object> value = (Map<String, Object>) item.get("value");
        if (value.containsKey("magnitude")){
            value.put("magnitude",20.0);
        }

        //从工厂中获得KieServices实例
        KieServices kieServices = KieServices.Factory.get();
        //默认自动加载 META-INF/kmodule.xml
        //从KieServices中获得KieContainer实例，其会加载kmodule.xml文件并load规则文件
        KieContainer kieContainer = kieServices.getKieClasspathContainer();
        //kmodule.xml 中定义的 ksession name
        //建立KieSession到规则文件的通信管道
        //kieSession有状态, 维护会话状态，type=stateful  最后结束要调用dispose()
        //statelessKieSession无状态，不会维护会话状态 type=stateless
        KieSession kieSession = kieContainer.newKieSession("ksession-rules");
        //数据模型初始化test.drl

        kieSession.insert(structure);
        kieSession.fireAllRules();
        kieSession.dispose();

    }

}
