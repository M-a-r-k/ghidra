/* ###
 * IP: GHIDRA
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package agent.frida.model.impl;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

import org.apache.commons.lang3.tuple.Pair;

import agent.frida.model.iface1.FridaModelTargetConfigurable;
import agent.frida.model.iface2.*;
import ghidra.async.AsyncUtils;
import ghidra.dbg.DebuggerObjectModel.RefreshBehavior;
import ghidra.dbg.error.DebuggerIllegalArgumentException;
import ghidra.dbg.target.TargetConfigurable;
import ghidra.dbg.target.TargetObject;
import ghidra.dbg.target.schema.*;
import ghidra.dbg.target.schema.TargetObjectSchema.ResyncMode;
import ghidra.util.datastruct.WeakValueHashMap;

@TargetObjectSchemaInfo(
	name = "AvailableDevicesContainer",
	elements = {
		@TargetElementType(type = FridaModelTargetAvailableDeviceImpl.class) //
	},
	elementResync = ResyncMode.ALWAYS,
	attributes = { //
		@TargetAttributeType(name = TargetConfigurable.BASE_ATTRIBUTE_NAME, type = Integer.class), //
		@TargetAttributeType(type = Void.class)  //
	},
	canonicalContainer = true)
public class FridaModelTargetAvailableDevicesContainerImpl extends FridaModelTargetObjectImpl
		implements FridaModelTargetAvailableDevicesContainer, FridaModelTargetConfigurable {

	protected final Map<String, FridaModelTargetAvailableDevice> attachablesById =
		new WeakValueHashMap<>();

	public FridaModelTargetAvailableDevicesContainerImpl(FridaModelTargetRoot root) {
		super(root.getModel(), root, "AvailableDevices", "AvailableDevicesContainer");
		this.changeAttributes(List.of(), Map.of(BASE_ATTRIBUTE_NAME, 16), "Initialized");
	}

	@Override
	public CompletableFuture<Void> requestElements(RefreshBehavior refresh) {
		return getManager().listAvailableDevices().thenAccept(list -> {
			List<TargetObject> available;
			synchronized (this) {
				// NOTE: If more details added to entries, should clear attachablesById
				available =
					list.stream().map(this::getTargetAttachableEx).collect(Collectors.toList());
			}
			setElements(available, Map.of(), "Refreshed");
		});
	}

	public synchronized FridaModelTargetAvailableDevice getTargetAttachableEx(
			Pair<String, String> pair) {
		return attachablesById.computeIfAbsent(pair.getLeft(),
			i -> new FridaModelTargetAvailableDeviceImpl(this, pair.getLeft(), pair.getRight()));
	}

	@Override
	public synchronized FridaModelTargetAvailableDevice getTargetAttachable(String pid) {
		return attachablesById.computeIfAbsent(pid,
			i -> new FridaModelTargetAvailableDeviceImpl(this, pid));
	}

	@Override
	public CompletableFuture<Void> writeConfigurationOption(String key, Object value) {
		switch (key) {
			case BASE_ATTRIBUTE_NAME:
				if (value instanceof Integer) {
					this.changeAttributes(List.of(), Map.of(BASE_ATTRIBUTE_NAME, value),
						"Modified");
					for (FridaModelTargetAvailableDevice child : attachablesById.values()) {
						child.setBase(value);
					}
				}
				else {
					throw new DebuggerIllegalArgumentException("Base should be numeric");
				}
			default:
		}
		return AsyncUtils.nil();
	}

}
