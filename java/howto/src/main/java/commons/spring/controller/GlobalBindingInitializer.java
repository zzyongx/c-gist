package commons.spring.controller;

import org.springframework.format.support.DefaultFormattingConversionService;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.InitBinder;
import org.springframework.web.bind.WebDataBinder;
import commons.spring.format.JsonObjectClassAnnotationFormatterFactory;

@ControllerAdvice
public class GlobalBindingInitializer {
  @InitBinder
  public void initJsonObjectListbinder(WebDataBinder binder) {
    DefaultFormattingConversionService cs = (DefaultFormattingConversionService) binder.getConversionService();
    cs.addFormatterForFieldAnnotation(new JsonObjectClassAnnotationFormatterFactory());
  }
}
