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
package ghidra.app.plugin.core.debug.platform.gdb;

import java.util.*;

import ghidra.app.plugin.core.debug.service.model.launch.*;
import ghidra.dbg.DebuggerModelFactory;
import ghidra.dbg.util.ConfigurableFactory.Property;
import ghidra.debug.api.model.DebuggerProgramLaunchOffer;
import ghidra.dbg.util.PathUtils;
import ghidra.framework.plugintool.PluginTool;
import ghidra.program.model.listing.Program;

public class GdbDebuggerProgramLaunchOpinion extends AbstractDebuggerProgramLaunchOpinion {
	protected static final List<Class<? extends DebuggerProgramLaunchOffer>> OFFER_CLASSES =
		List.of(
			InVmGdbDebuggerProgramLaunchOffer.class,
			GadpGdbDebuggerProgramLaunchOffer.class,
			SshGdbDebuggerProgramLaunchOffer.class);

	protected static abstract class AbstractGdbDebuggerProgramLaunchOffer
			extends AbstractDebuggerProgramLaunchOffer {

		public AbstractGdbDebuggerProgramLaunchOffer(Program program, PluginTool tool,
				DebuggerModelFactory factory) {
			super(program, tool, factory);
		}

		@Override
		protected List<String> getLauncherPath() {
			return PathUtils.parse("Inferiors[1]");
		}

	}

	@FactoryClass("agent.gdb.GdbInJvmDebuggerModelFactory")
	protected static class InVmGdbDebuggerProgramLaunchOffer
			extends AbstractGdbDebuggerProgramLaunchOffer {

		public InVmGdbDebuggerProgramLaunchOffer(Program program, PluginTool tool,
				DebuggerModelFactory factory) {
			super(program, tool, factory);
		}

		@Override
		public String getConfigName() {
			return "IN-VM GDB";
		}

		@Override
		public String getMenuTitle() {
			return "in GDB locally IN-VM";
		}
	}

	@FactoryClass("agent.gdb.gadp.GdbGadpDebuggerModelFactory")
	protected static class GadpGdbDebuggerProgramLaunchOffer
			extends AbstractGdbDebuggerProgramLaunchOffer {

		public GadpGdbDebuggerProgramLaunchOffer(Program program, PluginTool tool,
				DebuggerModelFactory factory) {
			super(program, tool, factory);
		}

		@Override
		public String getConfigName() {
			return "GADP GDB";
		}

		@Override
		public String getMenuTitle() {
			return "in GDB locally via GADP";
		}
	}

	@FactoryClass("agent.gdb.GdbOverSshDebuggerModelFactory")
	protected static class SshGdbDebuggerProgramLaunchOffer
			extends AbstractGdbDebuggerProgramLaunchOffer {

		public SshGdbDebuggerProgramLaunchOffer(Program program, PluginTool tool,
				DebuggerModelFactory factory) {
			super(program, tool, factory);
		}

		@Override
		public String getConfigName() {
			return "SSH GDB";
		}

		@Override
		public String getQuickTitle() {
			Map<String, Property<?>> opts = factory.getOptions();
			return String.format("in GDB via ssh:%s@%s",
				opts.get("SSH username").getValue(),
				opts.get("SSH hostname").getValue());
		}

		@Override
		public String getMenuTitle() {
			return "in GDB via ssh";
		}
	}

	@Override
	protected Collection<Class<? extends DebuggerProgramLaunchOffer>> getOfferClasses() {
		return OFFER_CLASSES;
	}
}
