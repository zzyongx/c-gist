package commons.jsondoc;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.HashSet;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import com.google.common.collect.Sets;
import org.jsondoc.core.pojo.ApiMethodDoc;
import org.jsondoc.core.pojo.JSONDocTemplate;
import org.jsondoc.core.util.JSONDocUtils;
import org.jsondoc.core.annotation.ApiBodyObject;
import org.jsondoc.springmvc.scanner.AbstractSpringJSONDocScanner;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestMapping;

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

  private void appendSubCandidates(Class<?> clazz, Set<Class<?>> subCandidates) {
    if(clazz.isPrimitive() || clazz.equals(Class.class)) {
      return;
    }

    for (Field field : clazz.getDeclaredFields()) {
      Class fieldClass = field.getType();
      Set<Class<?>> fieldCandidates = new HashSet<Class<?>>();
      buildJSONDocObjectsCandidates(fieldCandidates, fieldClass, field.getGenericType());

      for(Class<?> candidate: fieldCandidates) {
        if(!subCandidates.contains(candidate)) {
          subCandidates.add(candidate);
          appendSubCandidates(candidate, subCandidates);
        }
      }
    }
  }

  @Override
  public Set<Class<?>> jsondocObjects(List<String> packages) {
    Set<Method> methodsAnnotatedWith = reflections.getMethodsAnnotatedWith(RequestMapping.class);
    Set<Class<?>> candidates = Sets.newHashSet();
    Set<Class<?>> subCandidates = Sets.newHashSet();
    Set<Class<?>> elected = Sets.newHashSet();
    
    for (Method method : methodsAnnotatedWith) {
      buildJSONDocObjectsCandidates(candidates, method.getReturnType(), method.getGenericReturnType());
      Integer requestBodyParameterIndex = JSONDocUtils.getIndexOfParameterWithAnnotation(method, ApiBodyObject.class);
      if(requestBodyParameterIndex != -1) {
        candidates.addAll(buildJSONDocObjectsCandidates(candidates, method.getParameterTypes()[requestBodyParameterIndex], method.getGenericParameterTypes()[requestBodyParameterIndex]));
      }
    }
    
    // This is to get objects' fields that are not returned nor part of the body request of a method, but that are a field
    // of an object returned or a body  of a request of a method
    for (Class<?> clazz : candidates) {
      appendSubCandidates(clazz, subCandidates);
    }

    candidates.addAll(subCandidates);
    
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
