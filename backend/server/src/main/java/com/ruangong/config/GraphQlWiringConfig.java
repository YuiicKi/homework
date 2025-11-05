package com.ruangong.config;

import com.ruangong.model.AdminProfileModel;
import com.ruangong.model.StudentProfileModel;
import com.ruangong.model.TeacherProfileModel;
import graphql.schema.TypeResolver;
import graphql.schema.idl.RuntimeWiring;
import graphql.schema.idl.TypeRuntimeWiring;
import org.springframework.context.annotation.Configuration;
import org.springframework.graphql.execution.RuntimeWiringConfigurer;

@Configuration
public class GraphQlWiringConfig implements RuntimeWiringConfigurer {

    @Override
    public void configure(RuntimeWiring.Builder builder) {
        builder.type(TypeRuntimeWiring.newTypeWiring("UserProfile")
            .typeResolver(userProfileTypeResolver()));
    }

    private TypeResolver userProfileTypeResolver() {
        return env -> {
            Object source = env.getObject();
            if (source instanceof AdminProfileModel) {
                return env.getSchema().getObjectType("AdminProfile");
            }
            if (source instanceof TeacherProfileModel) {
                return env.getSchema().getObjectType("TeacherProfile");
            }
            if (source instanceof StudentProfileModel) {
                return env.getSchema().getObjectType("StudentProfile");
            }
            return null;
        };
    }
}
