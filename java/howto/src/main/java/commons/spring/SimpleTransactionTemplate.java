package commons.spring;

import java.util.concurrent.Callable;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionCallbackWithoutResult;

public class SimpleTransactionTemplate {
  TransactionTemplate tt;
  
  public SimpleTransactionTemplate(TransactionTemplate tt) {
    this.tt = tt;
  }

  public void run(Runnable f) {
    tt.execute(new TransactionCallbackWithoutResult() {
        protected void doInTransactionWithoutResult(TransactionStatus status) {
          try {
            f.run();
          } catch (RuntimeException e) {
            throw e;
          } catch (Exception e) {
            throw new RuntimeException(e);
          }
        }
      });
  }
  
  @SuppressWarnings("unchecked")
  public <R> R call(Callable<R> f) {
    return (R) tt.execute(new TransactionCallback() {
        public Object doInTransaction(TransactionStatus status) {
          try {
            return f.call();
          } catch (RuntimeException e) {
            throw e;
          } catch (Exception e) {
            throw new RuntimeException(e);
          }
        }
      });
  }
}
