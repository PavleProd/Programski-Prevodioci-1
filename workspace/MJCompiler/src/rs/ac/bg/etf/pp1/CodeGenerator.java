package rs.ac.bg.etf.pp1;

import java.util.ArrayList;
import java.util.Stack;

import rs.ac.bg.etf.pp1.SemanticAnalyzer.*;
import rs.ac.bg.etf.pp1.ast.*;
import rs.etf.pp1.mj.runtime.Code;
import rs.etf.pp1.symboltable.Tab;
import rs.etf.pp1.symboltable.concepts.Obj;

public class CodeGenerator extends VisitorAdaptor {
	
	private int varCount;
	private int paramCnt;
	
	class ForLoop
	{
		int condLocation, updateValueLocation;
		ArrayList<Integer> breakLocations = new ArrayList<>();
	}
	
	class Condition
	{
		ArrayList<Integer> thenLocations = new ArrayList<>();
		ArrayList<Integer> elseLocations = new ArrayList<>();
	}
	
	private Stack<ForLoop> forLoopStack = new Stack<>();
	private Stack<Condition> conditionStack = new Stack<>();
	
	// PROGRAM
	
	@Override
	public void visit(Program prog) // kraj programa
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
				return; // ?? sto samo return
			}
			
			int offset = designator.obj.getAdr() - Code.pc;
			Code.put(Code.call);
			Code.put2(offset); // sto 2, ima na vezbama
			
			if(designator.obj.getType() != Tab.noType) // ako povratni tip nije void
			{
				Code.put(Code.pop); // mozda ne treba ??
				// ?? TREBA KAD JE BREAK DA SE POSLE NAMESTI MESTO NA KOJE SE SKACE(KAD ZNAMO KRAJ PETLJE)
				// ZATO JE ON PRAVIO ARRAY LSITE POGLEDAJ
			}
		}
		else if(option instanceof DesignatorStatementOptions_Inc) //designator++
		{
			// saberemo sa 1, smestimo u designator
			Code.loadConst(1);
			Code.put(Code.add);
			Code.store(designator.obj);
		}
		else if(option instanceof DesignatorStatementOptions_Dec) //designator--
		{
			// oduzmemo 1, smestimo u designator
			Code.loadConst(1);
			Code.put(Code.sub);
			Code.store(designator.obj);
		}
		else if(option instanceof DesignatorStatementOptions_AssignOpExpr) // designator = expr
		{
			Code.store(designator.obj);
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
	
	// USLOVNI SKOKOVI
	
	public void visit(ExprRelopOptional_Define relopExpr)
	{
		Relop relop = relopExpr.getRelop();
		
		int operation = -1;
		if(relop instanceof Relop_Double_Equal)
		{
			operation = Code.eq;
		}
		else if(relop instanceof Relop_Not_Equal)
		{
			operation = Code.ne;
		}
		else if(relop instanceof Relop_GT)
		{
			operation = Code.gt;
		}
		else if(relop instanceof Relop_GTE)
		{
			operation = Code.ge;
		}
		else if(relop instanceof Relop_LT)
		{
			operation = Code.lt;
		}
		else if(relop instanceof Relop_LTE)
		{
			operation = Code.le;
		}
		
		if(operation == -1)
		{
			return;
		}
		
		Code.putFalseJump(operation, 0);
		// ?? condScopes.peek().elsePatchLocations.add(Code.pc - 2);
		
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
	
	// NIZOVI	
	
	@Override
	public void visit(Member_Array arr)
	{
		Designator designator = (Designator)arr.getParent();
		if(designator.getParent() instanceof DesignatorStatement_Designator_With_Options)
		{
			DesignatorStatement_Designator_With_Options options =
					(DesignatorStatement_Designator_With_Options)designator.getParent();
			
			DesignatorStatementOptions option = options.getDesignatorStatementOptions();
			
			if((option instanceof DesignatorStatementOptions_Inc) || (option instanceof DesignatorStatementOptions_Dec))
			{
				Code.put(Code.dup2); // dupliraju se poslednje dve reci na steku(arr i indeks) ??
			}
		}
	}
	
	@Override
	public void visit(Var_NoScope var)
	{
		Designator designator = (Designator)var.getParent();
		if(designator.getMember() instanceof Member_Array)
		{
			Code.load(var.obj); // niz, gde element??
		}
	}
	
	@Override
	public void visit(Var_Scope var)
	{
		Designator designator = (Designator)var.getParent();
		if(designator.getMember() instanceof Member_Array)
		{
			Code.load(var.obj); // niz, gde element??
		}
	}
	
	// FOR PETLJA
	
	@Override
	public void visit(ForBeforeCondition beforeCondition)
	{
		ForLoop loop = new ForLoop();
		loop.condLocation = Code.pc;
		this.forLoopStack.push(loop);
		
		this.conditionStack.push(new Condition());
	}
	
	@Override
	public void visit(ForAfterCondition afterCondition)
	{
		Code.putJump(0); // stavlja se 0 koja ce se posle zakrpiti
		
		forLoopStack.peek().updateValueLocation = Code.pc; // adresa gde je update vrednosti
		conditionStack.peek().thenLocations.add(Code.pc - 2); // adresa gde je provera uslova
	}
	
	@Override
	public void visit(ForHeader forHeader) // Pocetak tela petlje
	{
		Code.putJump(forLoopStack.peek().condLocation);
		Condition currentCondition = conditionStack.peek();
		
		for(int thenLocation : currentCondition.thenLocations)
		{
			Code.fixup(thenLocation); // umesto 0 stavljamo trenutnu adresu pc-a, telo petlje
		}
		
		currentCondition.thenLocations.clear(); // popravili smo sve lokacije, ne trebaju nam vise
	}
	
	@Override
	public void visit(Statement_Break breakStatement)
	{
		Code.putJump(0); // ne znamo jos uvek gde je izlaz iz petlje
		forLoopStack.peek().breakLocations.add(Code.pc - 2);
	}
	
	@Override
	public void visit(Statement_Continue continueStatement)
	{
		Code.putJump(forLoopStack.peek().updateValueLocation); // skok na update vrednosti
	}
	
	@Override
	public void visit(Statement_For forStatement) // kraj for petlje
	{
		ForLoop loop = forLoopStack.pop();
		Condition condition = conditionStack.pop();
		
		Code.putJump(loop.updateValueLocation); // skacemo na update vrednosti
		
		for(int breakLocation : loop.breakLocations)
		{
			Code.fixup(breakLocation);
		}
		
		for(int elseLocation : condition.elseLocations)
		{
			Code.fixup(elseLocation);
		}
	}
	
	// IF
	
	@Override
	public void visit(IfBeforeCondition beforeCondition)
	{
		conditionStack.push(new Condition());
	}
	
	@Override
	public void visit(IfAfterCondition afterCondition)
	{
		Condition currentCondition = conditionStack.peek();
		
		for(int thenLocation : currentCondition.thenLocations)
		{
			Code.fixup(thenLocation);
		}
		
		currentCondition.thenLocations.clear();
	}
	
	@Override
	public void visit(IfThenStart start)
	{
		
	}
	
	@Override
	public void visit(IfThenEnd end) // kraj if-a (pocetak else-a opciono)
	{
		Condition currentCondition = conditionStack.peak();
		
		for(int elseLocation : currentCondition.elseLocations)
		{
			Code.fixup(elseLocation);
		}
		currentCondition.thenLocations.add(Code.pc - 2);
	}
	
	@Override
	public void visit()
	
}
