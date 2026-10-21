package dev.davidcaetano.umbra_api.arquitetura;

import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaGenericArrayType;
import com.tngtech.archunit.core.domain.JavaMethod;
import com.tngtech.archunit.core.domain.JavaParameterizedType;
import com.tngtech.archunit.core.domain.JavaType;
import com.tngtech.archunit.core.domain.JavaWildcardType;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchCondition;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.lang.ConditionEvents;
import com.tngtech.archunit.lang.SimpleConditionEvent;
import dev.davidcaetano.umbra_api.UmbraApiApplication;
import dev.davidcaetano.umbra_api.catalogo.enums.OrigemColeta;
import dev.davidcaetano.umbra_api.coleta.Scraper;
import jakarta.persistence.Entity;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.data.repository.Repository;
import org.springframework.stereotype.Controller;

import java.util.stream.Stream;

import static com.tngtech.archunit.base.DescribedPredicate.describe;
import static com.tngtech.archunit.core.domain.AccessTarget.Predicates.declaredIn;
import static com.tngtech.archunit.core.domain.JavaAccess.Predicates.target;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.methods;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.archunit.library.dependencies.SlicesRuleDefinition.slices;

@AnalyzeClasses(packages = "dev.davidcaetano.umbra_api", importOptions = ImportOption.DoNotIncludeTests.class)
class ArchitectureRulesTest {

    @ArchTest
    static final ArchRule fonteSoDecididaNosColetores = noClasses()
            .that().resideOutsideOfPackages("..coleta.itad..", "..coleta.steam..", "..coleta.kabum..")
            .and().doNotBelongToAnyOf(OrigemColeta.class)
            .should().accessTargetWhere(target(declaredIn(OrigemColeta.class)))
            .because("decidir pela fonte fora do próprio coletor é violação de OCP; passar o valor adiante continua permitido. "
                    + "Referência a método herdado de Enum (OrigemColeta::name) escapa desta regra (ARCHITECTURE.md §3.3)");

    @ArchTest
    static final ArchRule scraperNaoRecusaMetodoDoContrato = noClasses()
            .that().implement(Scraper.class)
            .should().callConstructorWhere(target(declaredIn(UnsupportedOperationException.class)))
            .because("scraper que recusa um método do contrato quebra ISP e LSP, "
                    + "e obriga quem chama a saber qual implementação recebeu (ARCHITECTURE.md §3.3)");

    @ArchTest
    static final ArchRule controllerNaoDependeDeRepository = noClasses()
            .that().areMetaAnnotatedWith(Controller.class)
            .should().dependOnClassesThat().areAssignableTo(Repository.class)
            .because("controller fala com service, e service fala com repositório (ARCHITECTURE.md §3.1)")
            .allowEmptyShould(true);

    @ArchTest
    static final ArchRule entidadeForaDaAssinaturaDeController = methods()
            .that().areDeclaredInClassesThat().areMetaAnnotatedWith(Controller.class)
            .should(naoTerEntidadeNaAssinatura())
            .because("entidade JPA nunca sai do backend; o que atravessa a fronteira HTTP é DTO "
                    + "(ARCHITECTURE.md §3.1 e §3.2)")
            .allowEmptyShould(true);

    @ArchTest
    static final ArchRule tipoAninhadoSoPrivado = classes()
            .that().areMemberClasses()
            .and(describe("não estão dentro de classe anotada com @ConfigurationProperties",
                    classe -> classe.getEnclosingClass()
                            .map(contem -> !contem.isAnnotatedWith(ConfigurationProperties.class))
                            .orElse(true)))
            .should().bePrivate()
            .because("tipo aninhado que outra classe usa tem arquivo próprio; "
                    + "só properties que espelham o YAML continuam aninhadas (ARCHITECTURE.md §3.2, regras 3 e 4)");

    @ArchTest
    static final ArchRule moduloNaoDependeDeConfig = noClasses()
            .that().resideOutsideOfPackage("..umbra_api.config..")
            .and().doNotBelongToAnyOf(UmbraApiApplication.class)
            .should().dependOnClassesThat().resideInAPackage("..umbra_api.config..")
            .because("config monta a aplicação e pode depender dos módulos; "
                    + "módulo que depende de config inverte a direção (ARCHITECTURE.md §3.3)");

    @ArchTest
    static final ArchRule pacotesDeTopoSemCiclo = slices()
            .matching("dev.davidcaetano.umbra_api.(*)..")
            .should().beFreeOfCycles()
            .because("ciclo entre pacotes de topo impede entender ou mover um módulo sem o outro (ARCHITECTURE.md §3.3)");

    private static ArchCondition<JavaMethod> naoTerEntidadeNaAssinatura() {
        return new ArchCondition<>("não ter entidade JPA no tipo de retorno nem nos parâmetros") {
            @Override
            public void check(JavaMethod metodo, ConditionEvents eventos) {
                Stream.concat(Stream.of(metodo.getReturnType()), metodo.getParameterTypes().stream())
                        .filter(ArchitectureRulesTest::contemEntidade)
                        .forEach(tipo -> eventos.add(SimpleConditionEvent.violated(metodo,
                                "%s tem a entidade JPA na assinatura, em %s"
                                        .formatted(metodo.getFullName(), tipo.getName()))));
            }
        };
    }

    private static boolean contemEntidade(JavaType tipo) {
        return switch (tipo) {
            case JavaParameterizedType parametrizado -> contemEntidade(parametrizado.toErasure())
                    || parametrizado.getActualTypeArguments().stream().anyMatch(ArchitectureRulesTest::contemEntidade);
            case JavaWildcardType curinga -> Stream.concat(curinga.getUpperBounds().stream(),
                            curinga.getLowerBounds().stream())
                    .anyMatch(ArchitectureRulesTest::contemEntidade);
            case JavaGenericArrayType array -> contemEntidade(array.getComponentType());
            case JavaClass classe -> classe.getBaseComponentType().isAnnotatedWith(Entity.class);
            default -> false;
        };
    }
}
