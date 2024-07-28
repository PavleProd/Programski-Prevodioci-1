package rs.ac.bg.etf.pp1;

import rs.etf.pp1.symboltable.concepts.Struct;
import rs.etf.pp1.symboltable.structure.SymbolDataStructure;

public class StructCustom extends Struct {
	public StructCustom(int kind) {
		super(kind);
	}
	
	public StructCustom(int kind, StructCustom elemType) {
		super(kind, elemType);
	}
	
	public StructCustom(int kind, SymbolDataStructure members)
	{
		super(kind, members);
	}

	public static final int Bool = 5;
}