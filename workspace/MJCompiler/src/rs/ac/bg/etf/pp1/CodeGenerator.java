package rs.ac.bg.etf.pp1;

import rs.ac.bg.etf.pp1.SemanticAnalyzer.*;
import rs.ac.bg.etf.pp1.ast.*;
import rs.etf.pp1.mj.runtime.Code;
import rs.etf.pp1.symboltable.Tab;
import rs.etf.pp1.symboltable.concepts.Obj;

public class CodeGenerator extends VisitorAdaptor {
	
	private int varCount;
	private int paramCnt;
	
	// PROGRAM
	
	@Override
	public void visit(Program prog)
	{
		Code.dataSize = prog.getProgramName().obj.getLocalSymbols().size(); // broj simbola
	}
	
	// POCETAK METODE
	
	@Override
	public void visit(MethodDeclarationTypeAndName_Type method)
	{
		method.obj.setAdr(Code.pc);
		
		// enter brojArg, brojLokalnihProm + brojArg
		Code.put(Code.enter);
		Code.put(method.obj.getLevel());
		Code.put(method.obj.getLocalSymbols().size());
	}
	
	@Override
	public void visit(MethodDeclarationTypeAndName_Void method)
	{
		if(method.obj.getName().equals("main")) // main je void metoda
		{
			Code.mainPc = Code.pc;
		}
		
		method.obj.setAdr(Code.pc);
		
		// enter brojArg, brojLokalnihProm + brojArg
		Code.put(Code.enter);
		Code.put(method.obj.getLevel());
		Code.put(method.obj.getLocalSymbols().size());
	}
	
	// POZIV METODE
	
	@Override
	public void visit(DesignatorStatement_Designator_With_Options options)
	{
		DesignatorStatementOptions option = (DesignatorStatementOptions) options.getDesignatorStatementOptions();
		Designator designator = options.getDesignator();

		if(option instanceof DesignatorStatementOptions_ActPars) // poziv funkcije
		{
			String methodName = designator.obj.getName();
			
			switch(methodName)
			{
			case "len":
				Code.put(Code.arraylength);
				return;
			case "chr":
			case "ord":
				return;
			}
			
			int offset = designator.obj.getAdr() - Code.pc;
			Code.put(Code.call);
			Code.put2(offset); // sto 2, ima na vezbama
		}
	}
	
	@Override
	public void visit(Factor_Designator factor)
	{
		Designator designator = factor.getDesignator();
		if(factor.getActParsOptionalBrackets() instanceof ActParsOptionalBrackets_Define) // poziv funkcije
		{
			String methodName = designator.obj.getName();
			
			switch(methodName)
			{
			case "len":
				Code.put(Code.arraylength);
				return;
			case "chr":
			case "ord":
				return;
			}
			
			int offset = designator.obj.getAdr() - Code.pc;
			Code.put(Code.call);
			Code.put2(offset); // sto 2, ima na vezbama
		}
	}
	
	// POVRATAK IZ METODE
	
	@Override
	public void visit(Statement_Return ret)
	{
		Code.put(Code.exit);
		Code.put(Code.return_);
	}
	
	// KONSTANTE
	
	@Override
	public void visit(ConstValue_INT constVal)
	{
		// deo definicije konstante
		if(constVal.getParent() instanceof ConstDeclarationElement)
		{
			return;
		}
		
		Code.loadConst(constVal.getValue());
	}
	
	@Override
	public void visit(ConstValue_CHAR constVal)
	{
		// deo definicije konstante
		if(constVal.getParent() instanceof ConstDeclarationElement)
		{
			return;
		}
		
		Code.loadConst(constVal.getValue());
	}
	
	@Override
	public void visit(ConstValue_BOOL constVal)
	{
		// deo definicije konstante
		if(constVal.getParent() instanceof ConstDeclarationElement)
		{
			return;
		}
		
		int value = constVal.getValue() ? 1 : 0;
		Code.loadConst(value);
	}
	
	// PRINT
	
	public void visit(Statement_Print print)
	{
		if(print.getNumConstOptional() instanceof NumConstOptional_Define)
		{
			NumConstOptional_Define intConst = (NumConstOptional_Define)print.getNumConstOptional();
			Code.loadConst(intConst.getValue());
		}
		else
		{
			Code.loadConst(0);
		}

		if(print.getExpr().struct == Tab.intType)
		{
			Code.put(Code.print);
		}
		else
		{
			Code.put(Code.bprint);
		}
	}
	
	// READ
	
	public void visit(Statement_Read read)
	{
		Designator designator = read.getDesignator();
		
		if(designator.obj.getType() == Tab.intType)
		{
			Code.put(Code.read);
		}
		else
		{
			Code.put(Code.bread);
		}
		
		Code.store(designator.obj);
	}
	
	// FACTOR
	
	@Override
	public void visit(Factor_New factor)
	{
		Type type = factor.getType();
		FactorNew factorNew = factor.getFactorNew();
		if(factorNew instanceof FactorNew_Array)
		{
			FactorNew_Array newArray = (FactorNew_Array)factorNew;
			
			Code.put(Code.newarray);
			// kako se ubacuje velicina niza??
			// po dokumentaciji: niz velicine n, 0 - 1 bajt, 1 - velicina reci
			if(type.struct == Tab.charType)
			{
				Code.put(0);
			}
			else
			{
				Code.put(1);
			}
		}
	}
	
	// ARITMETICKE OPERACIJE
	
	@Override
	public void visit(AddOp_Plus operation)
	{
		Code.put(Code.add);
	}
	
	@Override
	public void visit(AddOp_Minus operation)
	{
		Code.put(Code.sub);
	}
	
	@Override
	public void visit(MulOp_Mul operation)
	{
		Code.put(Code.mul);
	}
	
	@Override
	public void visit(MulOp_Div operation)
	{
		Code.put(Code.div);
	}
	
	@Override
	public void visit(MulOp_Mod operation)
	{
		Code.put(Code.rem);
	}
	
	@Override
	public void visit(MinusOptional_Define operation)
	{
		Code.put(Code.neg);
	}
}
