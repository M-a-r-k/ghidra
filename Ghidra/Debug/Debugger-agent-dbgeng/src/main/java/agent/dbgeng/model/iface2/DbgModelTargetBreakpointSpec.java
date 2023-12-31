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
package agent.dbgeng.model.iface2;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import agent.dbgeng.manager.breakpoint.DbgBreakpointInfo;
import agent.dbgeng.model.iface1.DbgModelTargetBptHelper;
import ghidra.dbg.target.*;
import ghidra.dbg.target.TargetBreakpointSpecContainer.TargetBreakpointKindSet;
import ghidra.program.model.address.*;
import ghidra.util.Msg;

public interface DbgModelTargetBreakpointSpec extends //
		DbgModelTargetObject, //
		TargetBreakpointSpec, //
		TargetBreakpointLocation, //
		TargetDeletable, //
		DbgModelTargetBptHelper {

	String BPT_ACCESS_ATTRIBUTE_NAME = "Access";
	String BPT_DISP_ATTRIBUTE_NAME = "Disposition";
	String BPT_PENDING_ATTRIBUTE_NAME = "Pending";
	String BPT_TIMES_ATTRIBUTE_NAME = "Times";
	String BPT_TYPE_ATTRIBUTE_NAME = "Type";
	String BPT_INDEX_ATTRIBUTE_NAME = "Id";

	@Override
	public default CompletableFuture<Void> delete() {
		return getModel().gateFuture(getManager().deleteBreakpoints(getNumber()));
	}

	@Override
	public default CompletableFuture<Void> disable() {
		setEnabled(false, "Disabled");
		return getModel().gateFuture(getManager().disableBreakpoints(getNumber()));
	}

	@Override
	public default CompletableFuture<Void> enable() {
		setEnabled(true, "Enabled");
		return getModel().gateFuture(getManager().enableBreakpoints(getNumber()));
	}

	@Override
	public default String getExpression() {
		DbgBreakpointInfo info = getBreakpointInfo();
		return info == null ? null : info.getExpression();
	}

	public default long getNumber() {
		DbgBreakpointInfo info = getBreakpointInfo();
		return info == null ? null : info.getNumber();
	}

	@Override
	public default TargetBreakpointKindSet getKinds() {
		DbgBreakpointInfo info = getBreakpointInfo();
		if (info == null) {
			return TargetBreakpointKindSet.of();
		}
		switch (info.getType()) {
			case BREAKPOINT:
				return TargetBreakpointKindSet.of(TargetBreakpointKind.SW_EXECUTE);
			case HW_BREAKPOINT:
				return TargetBreakpointKindSet.of(TargetBreakpointKind.HW_EXECUTE);
			case HW_WATCHPOINT:
				return TargetBreakpointKindSet.of(TargetBreakpointKind.WRITE);
			case READ_WATCHPOINT:
				return TargetBreakpointKindSet.of(TargetBreakpointKind.READ);
			case ACCESS_WATCHPOINT:
				return TargetBreakpointKindSet.of(TargetBreakpointKind.READ,
					TargetBreakpointKind.WRITE);
			default:
				return TargetBreakpointKindSet.of();
		}
	}

	@Override
	public default CompletableFuture<Void> init(Map<String, Object> map) {
		AddressSpace space = getModel().getAddressSpace("ram");
		return requestNativeAttributes().thenAccept(attrs -> {
			if (attrs != null) {
				map.putAll(attrs);
				TargetObject addr = (TargetObject) attrs.get("Address");
				TargetObject id = (TargetObject) attrs.get("Id");
				//TargetObject unique = (TargetObject) attrs.get("UniqueID");
				TargetObject enabled = (TargetObject) attrs.get("IsEnabled");
				String addstr = addr.getCachedAttribute(VALUE_ATTRIBUTE_NAME).toString();
				String idstr = id.getCachedAttribute(VALUE_ATTRIBUTE_NAME).toString();
				setBreakpointId(idstr);
				//String uidstr = unique.getCachedAttribute(VALUE_ATTRIBUTE_NAME).toString();
				String enstr = enabled.getCachedAttribute(VALUE_ATTRIBUTE_NAME).toString();
				Address address = null;
				try {
					address = space.getAddress(addstr);
					//map.put(ADDRESS_ATTRIBUTE_NAME, address);
				}
				catch (AddressFormatException e) {
					Msg.error(this, "Could not parse breakpoint address", e);
				}
				map.put(SPEC_ATTRIBUTE_NAME, this);
				map.put(EXPRESSION_ATTRIBUTE_NAME, addstr);
				map.put(KINDS_ATTRIBUTE_NAME, getKinds());
				//map.put(BPT_INDEX_ATTRIBUTE_NAME, Long.decode(idstr));
				map.put(ENABLED_ATTRIBUTE_NAME, enstr.equals("-1"));
				setEnabled(enstr.equals("-1"), "Refreshed");
				int size = getBreakpointInfo().getSize();
				//map.put(LENGTH_ATTRIBUTE_NAME, size);
				if (address != null) {
					try {
						map.put(RANGE_ATTRIBUTE_NAME, new AddressRangeImpl(address, size));
					}
					catch (AddressOverflowException e) {
						Msg.error(this, "Address overflow in breakpoint range", e);
					}
				}

				String oldval = (String) getCachedAttribute(DISPLAY_ATTRIBUTE_NAME);
				String display = "[" + idstr + "] " + addstr;
				map.put(DISPLAY_ATTRIBUTE_NAME, display);
				setModified(map, !display.equals(oldval));
			}
		});
	}

	private long orZero(Long l) {
		if (l == null) {
			Msg.warn(this, "Breakpoint had null offset. Defaulting to ram:00000000");
		}
		return l == null ? 0 : l;
	}

	private int orOne(Integer i) {
		return i == null ? 1 : i;
	}

	public default Address doGetAddress() {
		DbgBreakpointInfo info = getBreakpointInfo();
		return getModel().getAddress("ram", orZero(info.getOffset()));
	}

	public default AddressRange doGetRange() {
		DbgBreakpointInfo info = getBreakpointInfo();
		AddressSpace ram = getModel().getAddressSpace("ram");
		Address min = ram.getAddress(orZero(info.getOffset()));
		Address max = min.add(orOne(info.getSize()) - 1);
		return new AddressRangeImpl(min, max);
	}

	public default void updateInfo(DbgBreakpointInfo oldInfo, DbgBreakpointInfo newInfo,
			String reason) {
		synchronized (this) {
			assert oldInfo == getBreakpointInfo();
			setBreakpointInfo(newInfo);
		}
		setEnabled(newInfo.isEnabled(), reason);
	}

	/**
	 * Update the enabled field
	 *
	 * <p>
	 * This does not actually toggle the breakpoint. It just updates the field and calls the proper
	 * listeners. To actually toggle the breakpoint, use {@link #toggle(boolean)} instead, which if
	 * effective, should eventually cause this method to be called.
	 *
	 * @param enabled true if enabled, false if disabled
	 * @param reason a description of the cause (not really used, yet)
	 */
	public default void setEnabled(boolean enabled, String reason) {
		setBreakpointEnabled(enabled);
		changeAttributes(List.of(), Map.of(ENABLED_ATTRIBUTE_NAME, enabled //
		), reason);
	}

	@Override
	public default boolean isEnabled() {
		return isBreakpointEnabled();
	}

	@Override
	public default void addAction(TargetBreakpointAction action) {
		getActions().add(action);
	}

	@Override
	public default void removeAction(TargetBreakpointAction action) {
		getActions().remove(action);
	}

	public default void breakpointHit() {
		DbgModelTargetThread targetThread =
			getParentProcess().getThreads().getTargetThread(getManager().getEventThread());
		getActions().invoke()
				.breakpointHit((DbgModelTargetBreakpointSpec) getProxy(), targetThread, null, this);
	}

}
