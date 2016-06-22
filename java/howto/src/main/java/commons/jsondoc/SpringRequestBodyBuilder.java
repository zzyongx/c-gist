package commons.jsondoc;

import java.lang.reflect.Method;
import org.jsondoc.core.pojo.ApiBodyObjectDoc;
import org.jsondoc.core.util.JSONDocType;
import org.jsondoc.core.util.JSONDocTypeBuilder;
import org.jsondoc.core.util.JSONDocUtils;
import org.jsondoc.core.annotation.ApiBodyObject;

public class SpringRequestBodyBuilder {

	public static ApiBodyObjectDoc buildRequestBody(Method method) {
		Integer index = JSONDocUtils.getIndexOfParameterWithAnnotation(method, ApiBodyObject.class);
		if (index != -1) {
			ApiBodyObjectDoc apiBodyObjectDoc = new ApiBodyObjectDoc(JSONDocTypeBuilder.build(new JSONDocType(), method.getParameterTypes()[index], method.getGenericParameterTypes()[index]));
			return apiBodyObjectDoc;
		}
		
		return null;
	}
	
}
