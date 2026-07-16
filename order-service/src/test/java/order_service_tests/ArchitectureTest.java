package order_service_tests;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;

import sap.shipping.common.exagonal.Adapter;
import sap.shipping.common.exagonal.InBoundPort;
import sap.shipping.common.exagonal.OutBoundPort;

import org.junit.jupiter.api.Test;
import static com.tngtech.archunit.library.Architectures.layeredArchitecture;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

public class ArchitectureTest {

    @Test
    public void cleanArchitecture() {
        JavaClasses importedClasses = new ClassFileImporter().importPackages("sap.shipping.order");

        var domainPackage = "..domain..";
        var applicationPackage = "..application..";
        var infrastructurePackage = "..infrastructure..";

        var domainModelWithNoDeps =
                noClasses().that().resideInAPackage(domainPackage)
                .should().dependOnClassesThat().resideInAPackage(applicationPackage)
                .orShould().dependOnClassesThat().resideInAPackage(infrastructurePackage);
        domainModelWithNoDeps.check(importedClasses);

        var layeredRule = layeredArchitecture()
            .consideringAllDependencies()
            .layer("Domain").definedBy(domainPackage)
            .layer("Application").definedBy(applicationPackage)
            .layer("Infrastructure").definedBy(infrastructurePackage)
            .whereLayer("Infrastructure").mayNotBeAccessedByAnyLayer()
            .whereLayer("Application").mayOnlyBeAccessedByLayers("Infrastructure")
            .whereLayer("Domain").mayOnlyBeAccessedByLayers("Application", "Infrastructure");
        layeredRule.check(importedClasses);
    }

    @Test
    public void hexagonalArchitecture() {
        cleanArchitecture();

        JavaClasses importedClasses = new ClassFileImporter().importPackages("sap.shipping.order");

        var applicationPackage = "..application..";
        var domainPackage = "..domain..";
        var infrastructurePackage = "..infrastructure..";

        var portsRule = classes().that()
                    .areAnnotatedWith(InBoundPort.class).or()
                    .areAnnotatedWith(OutBoundPort.class)
                    .should().resideInAPackage(applicationPackage)
                    .orShould().resideInAPackage(domainPackage);
        portsRule.check(importedClasses);

        var adaptersRule = classes().that()
                    .areAnnotatedWith(Adapter.class)
                    .should().resideInAPackage(infrastructurePackage);
        adaptersRule.check(importedClasses);
    }
}
