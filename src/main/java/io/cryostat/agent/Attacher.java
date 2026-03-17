/*
 * Copyright The Cryostat Authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.cryostat.agent;

import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import io.cryostat.agent.shaded.ShadeLogger;

import com.sun.tools.attach.VirtualMachine;
import com.sun.tools.attach.VirtualMachineDescriptor;

class Attacher {

    static final String ALL_PIDS = "*";
    static final String AUTO_ATTACH_PID = "0";

    void attach(Agent agent) throws Exception {
        List<String> pids = getAttachPid(agent.pid);
        if (pids.isEmpty()) {
            throw new IllegalStateException("No candidate JVM PIDs");
        }
        String agentmainArg =
                new AgentArgs(
                                agent.properties,
                                String.join(
                                        ",",
                                        Optional.ofNullable(agent.smartTriggers).orElse(List.of())))
                        .toAgentMain();
        for (String pid : pids) {
            VirtualMachine vm = VirtualMachine.attach(pid);
            ShadeLogger.getAnonymousLogger()
                    .fine(String.format("Injecting agent into PID %s", pid));
            try {
                vm.loadAgent(Path.of(selfJarLocation()).toAbsolutePath().toString(), agentmainArg);
            } finally {
                vm.detach();
            }
        }
    }

    private static List<String> getAttachPid(String pidSpec) {
        List<VirtualMachineDescriptor> vms = VirtualMachine.list();
        Predicate<VirtualMachineDescriptor> vmFilter;
        if (ALL_PIDS.equals(pidSpec)) {
            vmFilter = vmd -> true;
        } else if (pidSpec == null || AUTO_ATTACH_PID.equals(pidSpec)) {
            if (vms.size() > 2) { // one of them is ourself
                throw new IllegalStateException(
                        String.format(
                                "Too many available virtual machines. Auto-attach only progresses"
                                        + " if there is one candidate. VMs: %s",
                                vms));
            } else if (vms.size() < 2) {
                throw new IllegalStateException(
                        String.format(
                                "Too few available virtual machines. Auto-attach only progresses if"
                                        + " there is one candidate. VMs: %s",
                                vms));
            }
            long ownId = ProcessHandle.current().pid();
            vmFilter = vmd -> !Objects.equals(String.valueOf(ownId), vmd.id());
        } else {
            vmFilter = vmd -> pidSpec.equals(vmd.id());
        }
        return vms.stream()
                .filter(vmFilter)
                .peek(
                        vmd ->
                                ShadeLogger.getAnonymousLogger()
                                        .fine(
                                                String.format(
                                                        "Attaching to VM: %s %s",
                                                        vmd.displayName(), vmd.id())))
                .map(VirtualMachineDescriptor::id)
                .collect(Collectors.toList());
    }

    static URI selfJarLocation() throws URISyntaxException {
        return Agent.class.getProtectionDomain().getCodeSource().getLocation().toURI();
    }
}
