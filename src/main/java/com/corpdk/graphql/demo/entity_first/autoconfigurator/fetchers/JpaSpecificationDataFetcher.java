package com.corpdk.graphql.demo.entity_first.autoconfigurator.fetchers;

import com.corpdk.graphql.demo.entity_first.autoconfigurator.ConfigureFetchers;
import graphql.schema.DataFetcher;
import graphql.schema.DataFetchingEnvironment;
import graphql.schema.GraphQLArgument;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.springframework.core.ResolvableType;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.core.annotation.MergedAnnotations;
import org.springframework.data.domain.ScrollPosition;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Window;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.repository.NoRepositoryBean;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.core.RepositoryMetadata;
import org.springframework.data.repository.core.support.DefaultRepositoryMetadata;
import org.springframework.data.repository.query.FluentQuery;
import org.springframework.data.util.TypeInformation;
import org.springframework.graphql.data.GraphQlArgumentBinder;
import org.springframework.graphql.data.GraphQlRepository;
import org.springframework.graphql.data.pagination.CursorEncoder;
import org.springframework.graphql.data.pagination.CursorStrategy;
import org.springframework.graphql.data.query.ScrollPositionCursorStrategy;
import org.springframework.graphql.data.query.ScrollSubrange;
import org.springframework.graphql.execution.RuntimeWiringConfigurer;
import org.springframework.graphql.execution.SelfDescribingDataFetcher;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;
import org.springframework.validation.BindException;

import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;

import static com.corpdk.graphql.demo.entity_first.autoconfigurator.Helpers.buildPropertyPaths;
import static com.corpdk.graphql.demo.entity_first.autoconfigurator.Helpers.requiresProjection;

public abstract class JpaSpecificationDataFetcher<T> {
    private static final Log logger = LogFactory.getLog(JpaSpecificationDataFetcher.class);


    private final TypeInformation<T> domainType;

    private final GraphQlArgumentBinder argumentBinder;

    JpaSpecificationDataFetcher(TypeInformation<T> domainType) {
        this.domainType = domainType;
        this.argumentBinder = new GraphQlArgumentBinder();
    }

    @Nullable
    static String getGraphQlTypeName(@NotNull Object repository) {
        GraphQlRepository annotation =
                AnnotatedElementUtils.findMergedAnnotation(repository.getClass(), GraphQlRepository.class);

        if (annotation == null) {
            return null;
        }

        return (StringUtils.hasText(annotation.typeName()) ?
                annotation.typeName() : getDomainType(repository).getSimpleName());
    }

    @SuppressWarnings("unchecked")
    static <T> @NotNull Class<T> getDomainType(Object executor) {
        return (Class<T>) getRepositoryMetadata(executor).getDomainType();
    }

    @Contract("_ -> new")
    static @NotNull RepositoryMetadata getRepositoryMetadata(Object executor) {
        Assert.isInstanceOf(Repository.class, executor);

        Type[] genericInterfaces = executor.getClass().getGenericInterfaces();
        for (Type genericInterface : genericInterfaces) {
            Class<?> rawClass = ResolvableType.forType(genericInterface).getRawClass();
            if (rawClass == null || MergedAnnotations.from(rawClass).isPresent(NoRepositoryBean.class)) {
                continue;
            }
            if (Repository.class.isAssignableFrom(rawClass)) {
                return new DefaultRepositoryMetadata(rawClass);
            }
        }

        throw new IllegalArgumentException(
                String.format("Cannot resolve repository interface from %s", executor));
    }

    @Contract("_ -> new")
    public static <T, R extends T> @NotNull Builder<T, R> builder(JpaSpecificationExecutor<T> executor) {
        return new Builder<>(executor, getDomainType(executor));
    }

    public static @NotNull RuntimeWiringConfigurer autoRegistrationConfigurer(
            @NotNull List<JpaSpecificationExecutor<?>> executors) {
        logger.info("No of Executors: " + executors.size());
        return autoRegistrationConfigurer(executors, null, null);
    }

    public static @NotNull RuntimeWiringConfigurer autoRegistrationConfigurer(
            @NotNull List<JpaSpecificationExecutor<?>> executors,
            @Nullable CursorStrategy<ScrollPosition> cursorStrategy,
            @Nullable ScrollSubrange defaultScrollSubrange) {

        Map<String, ConfigureFetchers.DataFetcherFactory> factories = new HashMap<>();

        for (JpaSpecificationExecutor<?> executor : executors) {
            String typeName = getGraphQlTypeName(executor);
            if (typeName != null) {
                Builder<?, ?> builder = customize(executor, builder(executor)
                        .cursorStrategy(cursorStrategy)
                        .defaultScrollSubRange(20, next -> {
                            AtomicReference<ScrollPosition> scrollPosition = new AtomicReference<>();
                            if (defaultScrollSubrange != null) {
                                defaultScrollSubrange.position().ifPresent(scrollPosition::set);
                            } else {
                                scrollPosition.set(null);
                            }
                            return scrollPosition.get();
                        }));
                factories.put(typeName, new ConfigureFetchers.DataFetcherFactory() {
                    @Override
                    public DataFetcher<?> single() {
                        return builder.single();
                    }

                    @Override
                    public DataFetcher<?> many() {
                        return builder.many();
                    }

                    @Override
                    public DataFetcher<?> scrollable() {
                        return builder.scrollable();
                    }

                    @Override
                    public DataFetcher<?> count() {
                        return builder.count();
                    }
                });
            }
        }

        if (logger.isTraceEnabled()) {
            logger.trace("Auto-registration candidate typeNames " + factories.keySet());
        }

        return new ConfigureFetchers(factories);
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static Builder customize(JpaSpecificationExecutor<?> executor, Builder builder) {
        if (executor instanceof JpaSpecificationDataFetcher.JpaSpecificationBuilderCustomizer<?, ?> customizer) {
            return customizer.customize(builder);
        }
        return builder;
    }

    @Nullable
    private static String getArgumentName(@NotNull DataFetchingEnvironment environment) {
        Map<String, Object> arguments = environment.getArguments();
        List<GraphQLArgument> definedArguments = environment.getFieldDefinition().getArguments();
        if (definedArguments.size() == 1) {
            String name = definedArguments.getFirst().getName();
            if (arguments.get(name) instanceof Map<?, ?>) {
                return name;
            }
        }
        return null;
    }

    public String getDescription() {
        return "JpaSpecificationDataFetcher<" + this.domainType.getType().getName() + ">";
    }

    @Override
    public String toString() {
        return getDescription();
    }

    public Specification<T> createSpecificationFromFilter(@NotNull DataFetchingEnvironment environment) throws BindException {
        logger.info("Input Query: " + environment.getDocument());
        String name = getArgumentName(environment);
        logger.info("Argument Name: " + name);
        ResolvableType targetType = ResolvableType.forClass(domainType.getType());
        Object bind = this.argumentBinder.bind(environment, name, targetType);
        Assert.notNull(bind, "bind must not be null");
        Class<?> clazz = bind.getClass();
        logger.info(clazz.getSimpleName());
        logger.info(bind.toString());
        return (root, query, criteriaBuilder) -> criteriaBuilder.conjunction();
    }

    public interface JpaSpecificationBuilderCustomizer<T, R extends T> {

        Builder<T, R> customize(Builder<T, R> builder);

    }

    public static class Builder<T, R> {

        private final JpaSpecificationExecutor<T> executor;

        private final TypeInformation<T> domainType;

        private final Class<R> resultType;

        @Nullable
        private final CursorStrategy<ScrollPosition> cursorStrategy;

        @Nullable
        private final Integer defaultScrollCount;

        @Nullable
        private final Function<Boolean, ScrollPosition> defaultScrollPosition;

        private final Sort sort;

        @SuppressWarnings("unchecked")
        Builder(JpaSpecificationExecutor<T> executor, Class<R> domainType) {
            this(executor, TypeInformation.of((Class<T>) domainType), domainType, null, null, null, Sort.unsorted());
        }

        Builder(JpaSpecificationExecutor<T> executor, TypeInformation<T> domainType, Class<R> resultType,
                @Nullable CursorStrategy<ScrollPosition> cursorStrategy,
                @Nullable Integer defaultScrollCount, @Nullable Function<Boolean, ScrollPosition> defaultScrollPosition,
                Sort sort) {
            logger.debug("Data Fetcher Builder Default Scroll Position: " + defaultScrollPosition);
            this.executor = executor;
            this.domainType = domainType;
            this.resultType = resultType;
            this.cursorStrategy = cursorStrategy;
            this.defaultScrollCount = defaultScrollCount;
            this.defaultScrollPosition = defaultScrollPosition;
            this.sort = sort;
        }

        public <P> Builder<T, P> projectAs(Class<P> projectionType) {
            Assert.notNull(projectionType, "Projection type must not be null");
            return new Builder<>(this.executor, this.domainType, projectionType,
                    this.cursorStrategy, this.defaultScrollCount, this.defaultScrollPosition, this.sort);
        }

        public Builder<T, R> cursorStrategy(@Nullable CursorStrategy<ScrollPosition> cursorStrategy) {
            return new Builder<>(this.executor, this.domainType, this.resultType,
                    cursorStrategy, this.defaultScrollCount, this.defaultScrollPosition, this.sort);
        }

        public Builder<T, R> defaultScrollSubRange(
                int defaultCount, Function<Boolean, ScrollPosition> defaultPosition) {

            return new Builder<>(this.executor, this.domainType,
                    this.resultType, this.cursorStrategy, defaultCount, defaultPosition, this.sort);
        }

        public Builder<T, R> sortBy(Sort sort) {
            Assert.notNull(sort, "Sort must not be null");
            return new Builder<>(this.executor, this.domainType, this.resultType,
                    this.cursorStrategy, this.defaultScrollCount, this.defaultScrollPosition, sort);
        }

        public DataFetcher<R> single() {
            return new FilterSingleEntityDataFetcher<>(this.executor, this.domainType, this.resultType, this.sort);
        }

        public DataFetcher<Iterable<R>> many() {
            return new FilterManyEntityDataFetcher<>(this.executor, this.domainType, this.resultType, this.sort);
        }

        public DataFetcher<Iterable<R>> scrollable() {
            logger.debug("Scrollable Data Fetcher Builder Default Scroll Position: " + this.defaultScrollPosition);
            return new FilterScrollableEntityDataFetcher<>(
                    this.executor, this.domainType, this.resultType,
                    (this.cursorStrategy != null) ? this.cursorStrategy : CursorStrategy.withEncoder(new ScrollPositionCursorStrategy(), CursorEncoder.base64()),
                    (this.defaultScrollCount != null) ? this.defaultScrollCount : 20,
                    (this.defaultScrollPosition != null) ? this.defaultScrollPosition : forward -> ScrollPosition.offset(),
                    this.sort);
        }

        public DataFetcher<Long> count() {
            return new FilterCountDataFetcher<>(this.executor, this.domainType);
        }

    }

    public static class FilterSingleEntityDataFetcher<T, R> extends JpaSpecificationDataFetcher<T> implements SelfDescribingDataFetcher<R> {

        private final JpaSpecificationExecutor<T> executor;

        private final TypeInformation<T> domainType;

        private final Class<R> resultType;

        private final Sort sort;

        public FilterSingleEntityDataFetcher(
                JpaSpecificationExecutor<T> executor, TypeInformation<T> domainType, Class<R> resultType, Sort sort) {
            super(domainType);
            this.domainType = domainType;
            this.executor = executor;
            this.resultType = resultType;
            this.sort = sort;
        }

        @Override
        public @NotNull String getDescription() {
            return "FilterSingleEntityDataFetcher<" + this.domainType.getType().getName() + ">";
        }

        @Override
        public @NotNull ResolvableType getReturnType() {
            return ResolvableType.forClass(this.resultType);
        }

        @Override
        @SuppressWarnings({"unchecked"})
        public R get(DataFetchingEnvironment environment) throws Exception {
            Specification<T> specification = createSpecificationFromFilter(environment);
            return this.executor.findBy(specification, query -> {
                FluentQuery.FetchableFluentQuery<R> queryToUse = (FluentQuery.FetchableFluentQuery<R>) query;

                if (this.sort.isSorted()) {
                    queryToUse = queryToUse.sortBy(this.sort);
                }

                Class<R> finalResultType = this.resultType;
                if (requiresProjection(this.domainType, finalResultType)) {
                    queryToUse = queryToUse.as(finalResultType);
                } else {
                    queryToUse = queryToUse.project(buildPropertyPaths(environment.getSelectionSet(), this.domainType, finalResultType));
                }

                return queryToUse.first();
            }).orElse(null);
        }
    }

    public static class FilterManyEntityDataFetcher<T, R> extends JpaSpecificationDataFetcher<T> implements SelfDescribingDataFetcher<Iterable<R>> {

        private final JpaSpecificationExecutor<T> executor;

        private final TypeInformation<T> domainType;

        private final Class<R> resultType;

        private final Sort sort;

        public FilterManyEntityDataFetcher(
                JpaSpecificationExecutor<T> executor, TypeInformation<T> domainType, Class<R> resultType, Sort sort) {
            super(domainType);
            this.domainType = domainType;
            this.resultType = resultType;
            this.executor = executor;
            this.sort = sort;
        }

        @Override
        public @NotNull String getDescription() {
            return this.getClass().getSimpleName() + "<" + this.domainType.getType().getName() + ">";
        }

        @Override
        public @NotNull ResolvableType getReturnType() {
            return ResolvableType.forClassWithGenerics(Iterable.class, this.domainType.getType());
        }

        @Override
        @SuppressWarnings("unchecked")
        public Iterable<R> get(DataFetchingEnvironment environment) throws Exception {
            Specification<T> specification = createSpecificationFromFilter(environment);
            return this.executor.findBy(specification, query -> {
                FluentQuery.FetchableFluentQuery<R> queryToUse = (FluentQuery.FetchableFluentQuery<R>) query;

                if (this.sort.isSorted()) {
                    queryToUse = queryToUse.sortBy(this.sort);
                }

                if (requiresProjection(this.domainType, this.resultType)) {
                    queryToUse = queryToUse.as(this.resultType);
                } else {
                    queryToUse = queryToUse.project(buildPropertyPaths(environment.getSelectionSet(), this.domainType, this.resultType));
                }

                return getResult(queryToUse, environment);
            });
        }

        /**
         * @param environment This {@link DataFetchingEnvironment} variable is overridden in scrollable implementation
         */
        protected Iterable<R> getResult(@NotNull FluentQuery.FetchableFluentQuery<R> queryToUse, DataFetchingEnvironment environment) {
            return queryToUse.all();
        }
    }

    public static class FilterScrollableEntityDataFetcher<T, R> extends FilterManyEntityDataFetcher<T, R> {

        private final CursorStrategy<ScrollPosition> cursorStrategy;

        private final int defaultCount;

        private final ResolvableType scrollableResultType;

        public FilterScrollableEntityDataFetcher(
                JpaSpecificationExecutor<T> executor, TypeInformation<T> domainType, Class<R> resultType, CursorStrategy<ScrollPosition> cursorStrategy,
                int defaultCount,
                Function<Boolean, ScrollPosition> defaultPosition,
                Sort sort) {
            super(executor, domainType, resultType, sort);

            Assert.notNull(cursorStrategy, "CursorStrategy is required");
            Assert.notNull(defaultPosition, "'defaultPosition' is required");

            this.cursorStrategy = cursorStrategy;
            this.defaultCount = defaultCount;
            this.scrollableResultType = ResolvableType.forClassWithGenerics(Window.class, domainType.getType());
        }

        @Override
        public @NotNull ResolvableType getReturnType() {
            return ResolvableType.forClassWithGenerics(Iterable.class, this.scrollableResultType);
        }

        @Override
        protected Iterable<R> getResult(@NotNull FluentQuery.FetchableFluentQuery<R> queryToUse, @NotNull DataFetchingEnvironment environment) {
            boolean forward = true;
            String cursor = environment.getArgument("after");
            Integer count = environment.getArgument("first");
            if (cursor == null && count == null) {
                cursor = environment.getArgument("before");
                count = environment.getArgument("last");
                if (cursor != null || count != null) {
                    forward = false;
                }
            }
            ScrollPosition pos = (cursor != null) ? cursorStrategy.fromCursor(cursor) : null;
            ScrollSubrange range = ScrollSubrange.create(pos, count, forward);
            count = range.count().orElse(this.defaultCount);
            logger.debug("Input ScrollPosition: " + range.position());
            AtomicReference<ScrollPosition> position = new AtomicReference<>();
            range.position().ifPresentOrElse(position::set, () -> position.set(ScrollPosition.offset()));
            logger.debug("Current ScrollPosition: " + position.get());
            return queryToUse.limit(count).scroll(position.get());
        }
    }

    public static class FilterCountDataFetcher<T> extends JpaSpecificationDataFetcher<T> implements SelfDescribingDataFetcher<Long> {

        private final JpaSpecificationExecutor<T> executor;

        private final TypeInformation<T> domainType;

        public FilterCountDataFetcher(
                JpaSpecificationExecutor<T> executor, TypeInformation<T> domainType) {
            super(domainType);
            this.domainType = domainType;
            this.executor = executor;
        }

        @Override
        public @NotNull String getDescription() {
            return this.getClass().getSimpleName() + "<" + this.domainType.getType().getName() + ">";
        }

        @Override
        public @NotNull ResolvableType getReturnType() {
            return ResolvableType.forClass(Long.class);
        }

        @Override
        public Long get(DataFetchingEnvironment environment) throws Exception {
            Specification<T> specification = createSpecificationFromFilter(environment);
            return this.executor.findBy(specification, FluentQuery.FetchableFluentQuery::count);
        }
    }
}
