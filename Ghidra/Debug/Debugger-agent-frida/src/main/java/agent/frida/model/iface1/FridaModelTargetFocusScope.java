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
package agent.frida.model.iface1;

import java.util.concurrent.CompletableFuture;

import agent.frida.model.iface2.FridaModelTargetObject;
import ghidra.async.AsyncUtils;
import ghidra.dbg.error.DebuggerIllegalArgumentException;
import ghidra.dbg.target.TargetFocusScope;
import ghidra.dbg.target.TargetObject;
import ghidra.dbg.util.PathUtils;

/**
 * An interface which indicates this object is capable of launching targets.
 * 
 * The targets this launcher creates ought to appear in its successors.
 * 
 * @param <T> type for this
 */
public interface FridaModelTargetFocusScope extends FridaModelTargetObject, TargetFocusScope {

	@Override
	public FridaModelSelectableObject getFocus();

	// NB: setFocus changes attributes - propagates up to client
	public boolean setFocus(FridaModelSelectableObject sel);

	// NB: requestFocus request change in focused object - propagates down to manager
	//  (but, of course, may then cause change in state)
	@Override
	public default CompletableFuture<Void> requestFocus(TargetObject obj) {
		return getModel().gateFuture(getManager().requestFocus(this, obj));
	}

	public default CompletableFuture<Void> doRequestFocus(TargetObject obj) {
		if (getManager().isWaiting()) {
			return AsyncUtils.nil();
		}
		getModel().assertMine(TargetObject.class, obj);
		if (obj.equals(getFocus())) {
			return AsyncUtils.nil();
		}
		if (!PathUtils.isAncestor(this.getPath(), obj.getPath())) {
			throw new DebuggerIllegalArgumentException("Can only focus a successor of the scope");
		}
		TargetObject cur = obj;
		while (cur != null) {
			if (cur instanceof FridaModelSelectableObject) {
				FridaModelSelectableObject sel = (FridaModelSelectableObject) cur;
				//System.err.println("requestFocus " + obj);
				setFocus(sel);
				return AsyncUtils.nil();
			}
			if (cur instanceof FridaModelTargetObject) {
				FridaModelTargetObject def = (FridaModelTargetObject) cur;
				cur = def.getParent();
				continue;
			}
			throw new AssertionError();
		}
		return AsyncUtils.nil();
	}

}
