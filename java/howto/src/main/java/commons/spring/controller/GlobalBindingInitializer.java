package commons.spring.controller;

import java.time.LocalDate;
import java.time.LocalDateTime;
import org.springframework.format.support.DefaultFormattingConversionService;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.InitBinder;
import org.springframework.web.bind.WebDataBinder;
import commons.spring.format.JsonObjectClassAnnotationFormatterFactory;
import commons.spring.format.Range;
import commons.spring.format.RangeEditor;

@ControllerAdvice
public class GlobalBindingInitializer {
  @InitBinder
  public void initJsonObjectListBinder(WebDataBinder binder) {
    DefaultFormattingConversionService cs = (DefaultFormattingConversionService) binder.getConversionService();
    cs.addFormatterForFieldAnnotation(new JsonObjectClassAnnotationFormatterFactory());
  }

  @InitBinder
  public void initRangeBinder(WebDataBinder binder) {
    binder.registerCustomEditor(Range.IntType.class, new RangeEditor<Integer>(Integer.class));
    binder.registerCustomEditor(Range.LongType.class, new RangeEditor<Long>(Long.class));
    binder.registerCustomEditor(Range.StringType.class, new RangeEditor<String>(String.class));
    binder.registerCustomEditor(Range.LocalDateType.class, new RangeEditor<LocalDate>(LocalDate.class));
    binder.registerCustomEditor(Range.LocalDateTimeType.class, new RangeEditor<LocalDateTime>(LocalDateTime.class));
    binder.registerCustomEditor(Range.DoubleType.class, new RangeEditor<Double>(Double.class));
  }
}
