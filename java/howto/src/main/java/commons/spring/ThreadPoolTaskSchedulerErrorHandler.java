package commons.spring;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.google.common.base.Throwables;
import org.springframework.util.ErrorHandler;

public class ThreadPoolTaskSchedulerErrorHandler implements ErrorHandler {
  private static final Logger logger = LoggerFactory.getLogger(ThreadPoolTaskSchedulerErrorHandler.class);

  @Override
  public void handleError(Throwable t) {
    logger.error(Throwables.getStackTraceAsString(t));
  }
}
