class ResourceHolder implements AutoCloseable {
  private String name;
  public ResourceHolder(String name) {
    this.name = name;
    System.out.println(name + " get resource");
  }
  public void useResource() throws Exception {
    throw new Exception(name + " exception in useResource");
  }
  public void close() {
    System.out.println(name + " release resource");
    throw new RuntimeException(name + " exception in close");
  }
}

public class TestException {
  public static void testTryBlock() throws Exception {
    ResourceHolder resourceHolder = null;
    try {
      resourceHolder = new ResourceHolder("try-block");
      resourceHolder.useResource();
    } catch (Exception e) {
      throw e;
    } finally {
      if (resourceHolder != null) resourceHolder.close();
    }
  }

  public static void testTryWithResource() throws Exception {
    try (
      ResourceHolder resourceHolder = new ResourceHolder("try-with-resource")
      ) {
      resourceHolder.useResource();
    } catch (Exception e) {
      throw e;
    }
  }
    
  public static void main(String[] args) {
    try {
      testTryBlock();
    } catch (Exception e) {
      System.out.println(e.toString());
    }
    try {
      testTryWithResource();
    } catch (Exception e) {
      System.out.println(e.toString());
    }
  }
}
