package com.clinicos.staff;

import org.junit.jupiter.api.Test;
import org.yaml.snakeyaml.Yaml;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
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

    @Test
    void everyImageUpdateWaitsForItsOwnRolloutBeforeTheNextService() throws Exception {
        List<String> services = List.of("clinic-patient", "clinic-appointment", "clinic-followup",
                "clinic-notification", "clinic-billing", "clinic-emr", "clinic-subscription",
                "clinic-staff", "clinic-feedback", "clinic-gateway");
        var updates = deploySteps().stream()
                .filter(step -> step.getOrDefault("run", "").toString().contains("set image"))
                .toList();
        assertThat(updates).hasSize(services.size());
        for (int i = 0; i < services.size(); i++) {
            var step = updates.get(i);
            String service = services.get(i);
            assertThat(step).doesNotContainKeys("continue-on-error", "if");
            assertThat(step.get("run").toString()).containsSubsequence(
                    "set image deployment/" + service,
                    service + "=$REGISTRY/" + service + ":${GITHUB_SHA}",
                    "kubectl -n $NAMESPACE rollout status deployment/" + service + " --timeout=12m");
        }
    }

    @Test
    void rolloutFailureStopsDeploymentWithoutUpdatingLaterServices() throws Exception {
        var script = new StringBuilder("""
                kubectl() {
                  if [[ "$*" == *"rollout status"* ]]; then
                    return 1
                  fi
                  echo image-updated
                }
                """);
        for (var step : deploySteps()) {
            if (step.getOrDefault("run", "").toString().contains("set image")) {
                script.append(step.get("run"));
            }
        }
        var process = new ProcessBuilder("bash", "-e", "-c", script.toString())
                .redirectErrorStream(true).start();
        String output = new String(process.getInputStream().readAllBytes(), java.nio.charset.StandardCharsets.UTF_8);
        assertThat(process.waitFor()).isNotZero();
        assertThat(output.lines().toList()).containsExactly("image-updated");
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
