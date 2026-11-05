package com.clinicos.staff;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.yaml.snakeyaml.Yaml;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class DeployGkeWorkflowTest {
    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> deploySteps() throws IOException {
        Path root = Path.of(System.getProperty("user.dir")).getParent();
        try (var input = Files.newInputStream(root.resolve(".github/workflows/deploy-gke.yml"))) {
            Map<String, Object> workflow = new Yaml().load(input);
            var jobs = (Map<String, Map<String, Object>>) workflow.get("jobs");
            return (List<Map<String, Object>>) jobs.get("deploy").get("steps");
        }
    }

    private String deploymentScript() throws IOException {
        var updates = deploySteps().stream()
                .filter(step -> step.getOrDefault("run", "").toString().contains("set image"))
                .toList();
        assertThat(updates).hasSize(1);
        var step = updates.getFirst();
        assertThat(step).doesNotContainKeys("continue-on-error", "if");
        return step.get("run").toString();
    }

    private Process runDeployment(String services, String failingCommand) throws Exception {
        String script = """
                kubectl() {
                  echo "kubectl $*"
                  if [[ -n "$FAILING_COMMAND" && "$*" == *"$FAILING_COMMAND"* ]]; then
                    return 1
                  fi
                  return 0
                }
                """ + deploymentScript();
        var builder = new ProcessBuilder("bash", "-e", "-c", script).redirectErrorStream(true);
        builder.environment().putAll(Map.of(
                "SERVICES", services,
                "NAMESPACE", "test-clinic",
                "REGISTRY", "example.invalid/clinicos",
                "GITHUB_SHA", "test-sha",
                "FAILING_COMMAND", failingCommand));
        return builder.start();
    }

    private List<String> kubectlCalls(Process process) throws IOException {
        String output = new String(process.getInputStream().readAllBytes(), java.nio.charset.StandardCharsets.UTF_8);
        return output.lines().filter(line -> line.startsWith("kubectl ")).toList();
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "clinic-patient clinic-appointment clinic-followup clinic-notification clinic-billing "
                    + "clinic-emr clinic-subscription clinic-staff clinic-feedback clinic-gateway",
            "clinic-staff clinic-gateway", "clinic-feedback", ""
    })
    void everyImageUpdateWaitsForItsOwnRolloutBeforeTheNextService(String services) throws Exception {
        var process = runDeployment(services, "");
        var calls = kubectlCalls(process);
        assertThat(process.waitFor()).isZero();
        var expected = new ArrayList<String>();
        for (String service : services.split(" ")) {
            if (!service.isEmpty()) {
                expected.add("kubectl -n test-clinic set image deployment/" + service
                        + " " + service + "=example.invalid/clinicos/" + service + ":test-sha");
                expected.add("kubectl -n test-clinic rollout status deployment/" + service + " --timeout=12m");
            }
        }
        assertThat(calls).containsExactlyElementsOf(expected);
    }

    @ParameterizedTest
    @ValueSource(strings = {"set image", "rollout status"})
    void deploymentFailureStopsWithoutUpdatingLaterServices(String failingCommand) throws Exception {
        var process = runDeployment("clinic-staff clinic-gateway", failingCommand);
        var calls = kubectlCalls(process);
        assertThat(process.waitFor()).isNotZero();
        var expected = new ArrayList<String>();
        expected.add("kubectl -n test-clinic set image deployment/clinic-staff "
                + "clinic-staff=example.invalid/clinicos/clinic-staff:test-sha");
        if (failingCommand.equals("rollout status")) {
            expected.add("kubectl -n test-clinic rollout status deployment/clinic-staff --timeout=12m");
        }
        assertThat(calls).containsExactlyElementsOf(expected);
    }

    @Test
    void diagnosticsRunOnFailureWithoutDumpingSecretResources() throws Exception {
        var diagnostics = deploySteps().stream()
                .filter(step -> "Show deployment failure diagnostics".equals(step.get("name")))
                .findFirst().orElseThrow();
        assertThat(diagnostics.get("if")).isEqualTo("failure()");
        assertThat(diagnostics.get("run").toString())
                .contains("get deployments", "get pods", "get events")
                .doesNotContain("get secrets", "describe pods");
    }
}
