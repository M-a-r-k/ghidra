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
package ghidra.app.util.bin.format.macho.commands;

import java.io.IOException;

import ghidra.app.util.bin.BinaryReader;
import ghidra.app.util.bin.format.macho.MachConstants;
import ghidra.program.model.data.*;
import ghidra.util.exception.DuplicateNameException;

/**
 * Represents an entry_point_command structure 
 */
public class EntryPointCommand extends LoadCommand {
	private long entryOffset;
	private long stackSize;

	EntryPointCommand(BinaryReader reader) throws IOException {
		super(reader);
		entryOffset = reader.readNextLong();
		stackSize = reader.readNextLong();
	}

	/**
	 * Returns the file (__TEXT) offset of main().
	 * @return the file (__TEXT) offset of main()
	 */
	public long getEntryOffset() {
		return entryOffset;
	}

	/**
	 * Return the initial stack size, if not zero.
	 * @return the initial stack size, if not zero
	 */
	public long getStackSize() {
		return stackSize;
	}

	@Override
	public DataType toDataType() throws DuplicateNameException, IOException {
		StructureDataType struct = new StructureDataType(getCommandName(), 0);
		struct.add(DWORD, "cmd", null);
		struct.add(DWORD, "cmdsize", null);
		struct.add(QWORD, "entryoff", null);
		struct.add(QWORD, "stacksize", null);
		struct.setCategoryPath(new CategoryPath(MachConstants.DATA_TYPE_CATEGORY));
		return struct;
	}

	@Override
	public String getCommandName() {
		return "entry_point_command";
	}

}
