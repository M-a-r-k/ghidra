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
package ghidra.trace.model.symbol;

import java.util.Collection;

import ghidra.program.model.symbol.Namespace;
import ghidra.trace.model.Trace;

/**
 * A trace namespace symbol.
 */
public interface TraceNamespaceSymbol extends TraceSymbol, Namespace {
	@Override
	Trace getTrace();

	@Override
	default TraceNamespaceSymbol getSymbol() {
		return this;
	}

	@Override
	default TraceNamespaceSymbol getObject() {
		return this;
	}

	@Override
	TraceNamespaceSymbol getParentNamespace();

	/**
	 * Get the children of this namespace
	 * 
	 * @return the children
	 */
	Collection<? extends TraceSymbol> getChildren();

	@Override
	String[] getPath();

	@Override
	boolean isGlobal();
}
