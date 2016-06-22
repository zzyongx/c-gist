package commons.jsondoc;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.lang.reflect.Method;
import com.google.common.collect.Sets;
import org.jsondoc.core.pojo.ApiMethodDoc;
import org.jsondoc.core.pojo.JSONDocTemplate;
import org.jsondoc.core.util.JSONDocUtils;
import org.jsondoc.core.annotation.ApiBodyObject;
import org.jsondoc.core.annotation.ApiObject;
import org.jsondoc.springmvc.scanner.AbstractSpringJSONDocScanner;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RestController;

public class Spring4xJSONDocScanner extends AbstractSpringJSONDocScanner {
  @Override
  public Set<Class<?>> jsondocControllers() {
    Set<Class<?>> jsondocControllers = reflections.getTypesAnnotatedWith(Controller.class, true);
    jsondocControllers.addAll(reflections.getTypesAnnotatedWith(RestController.class, true));
    return jsondocControllers;
  }

  @Override
  public ApiMethodDoc initApiMethodDoc(Method method, Map<Class<?>, JSONDocTemplate> jsondocTemplates) {
    ApiMethodDoc apiMethodDoc = super.initApiMethodDoc(method, jsondocTemplates);
    apiMethodDoc.setConsumes(SpringConsumesBuilder.buildConsumes(method));
    apiMethodDoc.setBodyobject(SpringRequestBodyBuilder.buildRequestBody(method));
    
    Integer index = JSONDocUtils.getIndexOfParameterWithAnnotation(method, ApiBodyObject.class);
    if (index != -1) {
      apiMethodDoc.getBodyobject().setJsondocTemplate(jsondocTemplates.get(method.getParameterTypes()[index]));
    }    
    return apiMethodDoc;
  }

  @Override
  public Set<Class<?>> jsondocObjects(List<String> packages) {
    Set<Class<?>> candidates = reflections.getTypesAnnotatedWith(ApiObject.class);
    Set<Class<?>> elected = Sets.newHashSet();
    
    for (Class<?> clazz : candidates) {
      if(clazz.getPackage() != null) {
        for (String pkg : packages) {
          if(clazz.getPackage().getName().contains(pkg)) {
            elected.add(clazz);
          }
        }
      }
    }
    
    return elected;
  }    
}
