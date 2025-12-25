package com.ruangong.config;

import graphql.GraphQLError;
import graphql.GraphqlErrorBuilder;
import graphql.schema.DataFetchingEnvironment;
import org.springframework.graphql.execution.DataFetcherExceptionResolverAdapter;
import org.springframework.graphql.execution.ErrorType;
import org.springframework.stereotype.Component;

@Component
public class GraphQlExceptionResolver extends DataFetcherExceptionResolverAdapter {

    @Override
    protected GraphQLError resolveToSingleError(Throwable exception, DataFetchingEnvironment environment) {
        if (exception instanceof IllegalArgumentException || exception instanceof IllegalStateException) {
            return GraphqlErrorBuilder.newError(environment)
                .message(exception.getMessage())
                .errorType(ErrorType.BAD_REQUEST)
                .build();
        }
        return null;
    }
}
