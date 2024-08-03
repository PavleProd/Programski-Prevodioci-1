package rs.ac.bg.etf.pp1;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.log4j.Logger;

import rs.ac.bg.etf.pp1.ast.*;
import rs.etf.pp1.symboltable.*;
import rs.etf.pp1.symboltable.concepts.*;

public class SemanticAnalyzer extends VisitorAdaptor {

	private boolean errorDetected = false;
	
	private Type currentType;
	private Obj currentMethod = null;
	private String currentNamespace = "";
	private int loopCount = 0;
	
	private ArrayList<Struct> args = new ArrayList<Struct>(); // argumenti funkcije koja ce biti pozvana
	private ArrayList<Obj> pars = new ArrayList<Obj>(); // paramtri trenutne funkcije

	Logger log = Logger.getLogger(getClass());

	public void reportError(String message, SyntaxNode info) {
		errorDetected = true;
		StringBuilder msg = new StringBuilder(message);
		int line = (info == null) ? 0: info.getLine();
		if (line != 0)
			msg.append (" na liniji ").append(line);
		log.error(msg.toString());
	}

	public void reportInfo(String message, SyntaxNode info) {
		StringBuilder msg = new StringBuilder(message); 
		int line = (info == null) ? 0: info.getLine();
		if (line != 0)
			msg.append (" na liniji ").append(line);
		log.info(msg.toString());
	}
	
	// STATISTIKA
	
	private int constCount = 0, globalVarCount = 0, localVarCount = 0, callsCount = 0;
	private int arrayAccessCount = 0, argumentUsageCount = 0;
	
	public void dump()
	{
		StringBuilder sb = new StringBuilder();

		sb.append("=====================================\n");
		sb.append("Broj Konstanti: ");
		sb.append(constCount);
		sb.append("\nBroj globalnih promenljivih: ");
		sb.append(globalVarCount);
		sb.append("\nBroj lokalnih promenljivih: ");
		sb.append(localVarCount);
		sb.append("\nBroj poziva funkcija: ");
		sb.append(callsCount);
		sb.append("\nBroj pristupa nizovima: ");
		sb.append(arrayAccessCount);
		sb.append("\nBroj koriscenja argumenata: ");
		sb.append(argumentUsageCount);
		sb.append("\n=====================================\n");

		System.out.println(sb);
	}
	
	// TYPE
	
	@Override
	public void visit(Type_NoScope type) 
	{
		String typeName = type.getTypeName();
		Obj typeNode = Tab.find(typeName);
		this.currentType = type;
		
		if(typeNode == Tab.noObj) // nije pronadjen tip
		{
			reportError("Nije pronadjen tip " + typeName + " u tabeli simbola!", null);
			type.struct = Tab.noType;
		}
		
		if(typeNode.getKind() != Obj.Type) // nije tip (nego promenljiva, klasa itd)
		{
			reportError("Greska: Ime " + typeName + " ne predstavlja tip!", type);
			type.struct = Tab.noType;
		}
		
		log.info("Tip: " + typeName);
		type.struct = typeNode.getType();
	}
	
	@Override
	public void visit(Type_Scope type)
	{
		this.currentType = type;
		
		String namespaceName = type.getNamespaceName();
		String typeName = type.getTypeName();
		String typeFullName = namespaceName + "::" + typeName;
		
		Obj namespaceNode = Tab.find(namespaceName);
		
		if(namespaceNode == Tab.noObj) // namespace ne postoji
		{
			reportError("Nije pronadjen namespace " + namespaceName + " u tabeli simbola!", type);
		}
		
		if(namespaceNode.getKind() != ObjCustom.Namespace) // tip nije namespace
		{
			reportError("Ime " + namespaceName + " ne predstavlja namespace!", type);
		}
		
		// provera da li namespace nije dobrog tipa?
		
		Obj typeNode = Tab.find(typeFullName);
		
		if(typeNode == Tab.noObj) // tip ne postoji
		{
			reportError("Nije pronadjen tip " + typeFullName + " u tabeli simbola!", type);
			type.struct = Tab.noType;
		}
		
		if(typeNode.getKind() != Obj.Type) // nije tip (nego promenljiva, klasa itd)
		{
			reportError("Ime " + typeFullName + " ne predstavlja tip!", type);
			type.struct = Tab.noType;
		}
		
		log.info("Tip: " + typeFullName);
		type.struct = typeNode.getType();
	}
	
	// VAR
	
	@Override
	public void visit(Var_NoScope var) 
	{
		String varName = var.getVarName();
		Obj varNode = Tab.find(varName);
		
		if(varNode == Tab.noObj) // nije pronadjen simbol
		{
			reportError("Nije pronadjen simbol " + varName + " u tabeli simbola!", null);
		}
		
		var.obj = varNode;
		
		if (this.pars.contains(var.obj))
		{
			++argumentUsageCount;
		}
			
		log.info("Var: " + varName);
	}
	
	@Override
	public void visit(Var_Scope var)
	{
		String namespaceName = var.getNamespaceName();
		String varName = namespaceName + "::" + var.getVarName();
		Obj varNode = Tab.find(varName);
		
		if(varNode == Tab.noObj) // nije pronadjen simbol
		{
			reportError("Nije pronadjen simbol " + varName + " u tabeli simbola!", null);
		}
		
		var.obj = varNode;
		
		log.info("Var: " + varName);
	}
	
	// PROGRAM
	
	@Override
	public void visit(Program program) // kraj programa
	{
		Obj mainMethod = Tab.find("main");
		if(mainMethod == Tab.noObj
					  || mainMethod.getKind() != Obj.Meth 
					  || mainMethod.getType() != Tab.noType
					  || mainMethod.getLevel() != 0)
		{
			reportError("Metoda main nije definisana!", program);
		}
		
		Tab.chainLocalSymbols(program.getProgramName().obj);
		Tab.closeScope();
	}

	@Override
	public void visit(ProgramName progName) // pocetak programa
	{
		// kind (enum simbola), name, type
		log.info("Obrada programa " + progName.getName());
		progName.obj = Tab.insert(Obj.Prog, progName.getName(), Tab.noType);
		Tab.openScope();     	
	}
	
	// NAMESPACE
	
	public void visit(NamespaceName namespaceName) // pocetak namespace-a
	{
		String name = namespaceName.getNamespaceName();
		log.info("Obrada namespacea: " + name);
	
		namespaceName.obj = Tab.insert(ObjCustom.Namespace, name, Tab.noType);
		
		Tab.openScope();
		
		this.currentNamespace = name;
	}
	
	public void visit(Namespace_Define namespace) // kraj namespace-a
	{
		//Tab.chainLocalSymbols(namespace.getNamespaceName().obj);
		//Tab.closeScope();
		
		this.currentNamespace = "";
	}
	
	
	// METHOD DECLARATION
	
	@Override
	public void visit(MethodDeclaration method)
	{
		// Navedeno da treba u vreme izvrsavanja!
		/*if(currentMethod.getType() != Tab.noType && !this.returnFound)
		{
			report_error("Ne postoji return iskaz!", null);
		}*/
		
		Tab.chainLocalSymbols(currentMethod);
		Tab.closeScope();
		
		this.pars.clear(); // brisanje parametara jer je kraj funkcije
		currentMethod = null;
	}
	
	public void visit(MethodDeclarationTypeAndName_Type typeAndName)
	{
		String namespacePrefix = (this.currentNamespace != "") ? (this.currentNamespace + "::") : "";

		String methodName = namespacePrefix + typeAndName.getMethodName();
		if(Tab.currentScope.findSymbol(methodName) != null) // metoda vec definisana u scope-u
		{
			reportError("Redefinicija metode " + methodName, null);
		}
		
		log.info("Obrada metode " + methodName);
		currentMethod = Tab.insert(Obj.Meth, methodName, this.currentType.struct);
		currentMethod.setLevel(0);
		typeAndName.obj = currentMethod;
		
		Tab.openScope();
	}
	
	@Override
	public void visit(MethodDeclarationTypeAndName_Void typeAndName)
	{
		String methodName = this.getNamespacePrefix() + typeAndName.getMethodName();
		if(Tab.currentScope.findSymbol(methodName) != null) // simbol vec definisan u scope-u
		{
			reportError("Redefinicija metode " + methodName, typeAndName);
		}
		
		log.info("Obrada metode " + methodName);
		currentMethod = Tab.insert(Obj.Meth, methodName, Tab.noType);
		currentMethod.setLevel(0);
		typeAndName.obj = currentMethod;
		
		Tab.openScope();
	}
	
	@Override
	public void visit(Parameter parameter)
	{
		String name = getNamespacePrefix() + parameter.getName();
		
		if(Tab.currentScope.findSymbol(name) != null)
		{
			reportError("Redefinicija promenljive " + name, parameter);
		}
		
		Struct type;
		if(parameter.getVarOrArray() instanceof VarOrArray_Array)
		{
			log.info("Obrada argumenta niza " + name);
			type = new Struct(Struct.Array, this.currentType.struct);
		}
		else
		{
			log.info("Obrada argumenta promenljive " + name);
			type = this.currentType.struct;
		}
		
		parameter.obj = Tab.insert(Obj.Var, name, type);
		
		// Povecavamo broj argumenata za 1
		this.currentMethod.setLevel(this.currentMethod.getLevel() + 1);
		
		this.pars.add(parameter.obj);
	}
	
	// CONST DECLARATION
	
	public void visit(ConstDeclarationElement constDecl)
	{
		String name = this.getNamespacePrefix() + constDecl.getVarName();
		log.info("Obrada konstante " + name);
		
		if(Tab.currentScope.findSymbol(name) != null)
		{
			reportError("Redifinicija promenljive " + name, constDecl);
		}
		
		Struct expectedType = this.currentType.struct;
		Struct actualType = constDecl.getConstValue().struct;
		
		if(expectedType != actualType)
		{
			reportError("Pogresan tip promenljive!", constDecl);
		}
		
		constDecl.obj = Tab.insert(Obj.Con, name, actualType);
		++this.constCount;
	}
	
	@Override
	public void visit(ConstValue_INT constVal)
	{
		constVal.struct = Tab.intType;
	}
	
	@Override
	public void visit(ConstValue_CHAR constVal)
	{
		constVal.struct = Tab.charType;
	}
	
	@Override
	public void visit(ConstValue_BOOL constVal)
	{
		constVal.struct = TabCustom.boolType;
	}
	
	// VAR DECLARATION
	
	public void visit(VarListElement element)
	{
		String name = getNamespacePrefix() + element.getElementName();
		
		if(Tab.currentScope.findSymbol(name) != null)
		{
			reportError("Redefinisanje promenljive " + name, element);
		}
		
		Struct struct;
		if(element.getVarOrArray() instanceof VarOrArray_Array)
		{
			log.info("Obrada niza " + name);
			struct = new Struct(Struct.Array, this.currentType.struct);
		}
		else
		{
			log.info("Obrada promenljive " + name);
			struct = this.currentType.struct;
		}
		
		element.obj = Tab.insert(Obj.Var, name, struct);
		
		if(this.currentMethod == null)
		{
			++globalVarCount;
		}
		else
		{
			++localVarCount;
		}
	}
	
	// FOR
	
	
	public void visit(For_Header forHeader)
	{
		++loopCount;
	}
	
	public void visit(Statement_For statement)
	{
		--loopCount;
	}
	
	// STATEMENT
	
	@Override
	public void visit(Statement_Break statement)
	{
		if(loopCount == 0)
		{
			reportError("Break naredba van for petlje! ", statement);
		}
	}
	
	@Override
	public void visit(Statement_Continue statement)
	{
		if(loopCount == 0)
		{
			reportError("Continue naredba van for petlje! ", statement);
		}
	}
	
	@Override
	public void visit(Statement_Return statement)
	{
		if(this.currentMethod == null)
		{
			reportError("Return naredba van metode! ", statement);
			return;
		}
		
		log.info("Povratak iz metode " + this.currentMethod.getName());
		
		if(this.currentMethod.getType() == Tab.noType) // void metoda
		{
			if(statement.getExprOptional() instanceof ExprOptional_Define)
			{
				reportError("Void metoda vraca ne-void vrednost! ", statement);
			}
		}
		else // ne-void metoda
		{
			if(this.currentMethod.getType() != statement.getExprOptional().struct) // razlicit povratni tip
			{
				reportError("Pogresan povratni tip metode! ", statement);
			}
		}
	}
	
	public void visit(Statement_Read statement)
	{
		Designator designator = statement.getDesignator();
		
		int[] allowedObjKinds = {Obj.Var, Obj.Elem};
		
		boolean isAllowed = false;
		for(int kind : allowedObjKinds)
		{
			if(designator.obj.getKind() == kind)
			{
				isAllowed = true;
				break;
			}
		}
		
		if(!isAllowed)
		{
			reportError("Dozvoljena je samo promenljiva ili clan niza!", null);
			return;
		}
		
		Struct[] allowedTypes = {Tab.intType, Tab.charType, TabCustom.boolType};
		isAllowed = false;
		for(Struct type : allowedTypes)
		{
			if(designator.obj.getType() == type)
			{
				isAllowed = true;
				break;
			}
		}
		
		if(!isAllowed)
		{
			reportError("Dozvoljeni tipovi: int, char, bool!", null);
			return;
		}
	}
	
	@Override
	public void visit(Statement_Print statement)
	{
		Struct[] allowedTypes = {Tab.intType, Tab.charType, TabCustom.boolType};
		boolean isAllowed = false;
		for(Struct type : allowedTypes)
		{
			if(statement.getExpr().struct == type)
			{
				isAllowed = true;
				break;
			}
		}
		
		if(!isAllowed)
		{
			reportError("Dozvoljeni tipovi: int, char, bool!", null);
			return;
		}
	}
	
	// EXPR
	
	@Override
	public void visit(ExprOptional_Define expr)
	{
		expr.struct = expr.getExpr().struct;
	}
	
	public void visit(Expr_TermMultiple expr)
	{
		Struct exprStruct = expr.getExpr().struct;
		Struct termStruct = expr.getTerm().struct;
		
		if(exprStruct != Tab.intType || termStruct != Tab.intType)
		{
			reportError("Pogresan tip rezultata izraza! Ocekivan int", expr);
		}
		
		expr.struct = Tab.intType;
	}
	
	public void visit(Expr_TermOne expr)
	{
		// samo kad imamo jedan samostalan Term sme da ne bude int
		expr.struct = expr.getTerm().struct;
	}
	
	public void visit(Expr_MinusTermOne expr)
	{
		Struct termStruct = expr.getTerm().struct;
		if(termStruct != Tab.intType)
		{
			reportError("Pogresan tip rezultata izraza! Ocekivan int", expr);
		}
		expr.struct = termStruct;
	}
	
	// TERM
	
	@Override
	public void visit(Term_One term)
	{
		term.struct = term.getFactor().struct;
	}
	
	@Override
	public void visit(Term_MultipleMulOp term)
	{
		if(term.getTerm().struct != Tab.intType || term.getFactor().struct != Tab.intType)
		{
			reportError("Pogresan tip izraza, ocekivan int", term);
			return;
		}
		
		term.struct = Tab.intType;
	}
	
	// POZIV FUNCKCIJE
	
	public void visit (ActParsOptional_Define actPars)
	{
		++callsCount;
	}
	
	public void visit(ActParsOptional_Skip actPars)
	{
		++callsCount;
	}
	
	@Override
	public void visit(Argument argument) // jedan argument funkcije
	{
		argument.struct = argument.getExpr().struct;
		this.args.add(argument.getExpr().struct);
	}
	
	public void visit(Factor_Designator factor) // opciono poziv funkcije
	{
		factor.struct = factor.getDesignator().obj.getType();
		Obj methodToBeCalled = factor.getDesignator().obj;
		
		if(factor.getActParsOptionalBrackets() instanceof ActParsOptionalBrackets_Skip)
		{
			return;
		}
		
		// ako je poziv f-je
		if(methodToBeCalled.getKind() != Obj.Meth)
		{
			reportError("Objekat nije metoda!", null);
			return;
		}		
		
		if(this.args.size() != methodToBeCalled.getLevel())
		{
			reportError("Razlicit broj argumenata metode i poziva metode!", factor);
			return;
		}
		
		// dohvatamo prvih getLevel (broj parametara) lokalnih simbola -> sve parametre
		List<Obj> params = methodToBeCalled.getLocalSymbols()
				.stream().limit(methodToBeCalled.getLevel()).toList();
		
		for(int i = 0; i < params.size(); ++i)
		{
			Struct argType = args.get(i);
			Struct paramType = params.get(i).getType();
			
			if(argType != paramType)
			{
				reportError("Parametar " + i + " je nekompatabilan sa argumentom funkcije", null);
			}
		}
		
		this.args.clear();
		log.info("Poziv funkcije " + methodToBeCalled.getName());

	}
	
	// FACTOR
	
	public void visit(Factor_Const factor)
	{
		factor.struct = factor.getConstValue().struct;
	}
	
	public void visit(Factor_Expr factor)
	{
		factor.struct = factor.getExpr().struct;
	}
	
	public void visit(Factor_New factor)
	{
		Struct arrayElementType = factor.getType().struct;

		FactorNew factorNew = factor.getFactorNew();
		if(factorNew instanceof FactorNew_Array)
		{
			FactorNew_Array factorNewArray = (FactorNew_Array)factorNew;
			if(factorNewArray.getExpr().struct != Tab.intType)
			{
				reportError("Pogresan tip izraza, ocekivan int", factor);
				return;
			}
		}
		
		factor.struct = new Struct(Struct.Array, arrayElementType);
	}
	
	// DESIGNATOR
	
	public void visit(Designator designator)
	{
		Members members = designator.getMembers();
		
		if(members instanceof Members_Skip) // designator je const, promenljiva, metoda
		{
			int[] allowedKinds = {Obj.Con, Obj.Var, Obj.Meth};
			int kind = designator.getVar().obj.getKind();
			boolean isAllowed = false;
			
			for(int allowedKind : allowedKinds)
			{
				if(kind == allowedKind)
				{
					designator.obj = designator.getVar().obj;
					isAllowed = true;
					return;
				}
			}
			
			if(!isAllowed)
			{
				reportError("Nedozvoljeni tip podataka!", null);
				return;
			}
		}
		else // designator je niz
		{
			Struct type = designator.getVar().obj.getType();
			
			if(type.getKind() != Struct.Array)
			{
				reportError("Pogresan tip izraza, ocekivan array", null);
				return;
			}
			
			++this.arrayAccessCount;
			designator.obj = new Obj(Obj.Elem, "", type.getElemType());
		}
		
		
	}
	
	@Override
	public void visit(Member_Array member)
	{
		if(member.getExpr().struct != Tab.intType)
		{
			reportError("Pogresan tip izraza, ocekivan int", null);
			return;
		}
	}
	
	// DESIGNATOR STATEMENT
	
	public void visit(DesignatorStatement_Designator_With_Options designatorStatement)
	{
		DesignatorStatementOptions options = designatorStatement.getDesignatorStatementOptions();
		Designator designator = designatorStatement.getDesignator();
		int designatorKind = designator.obj.getKind();
		
		if(options instanceof DesignatorStatementOptions_AssignOpExpr)
		{
			ArrayList<Integer> allowedKinds = new ArrayList<>(Arrays.asList(Obj.Var, Obj.Elem));
			//allowedKinds.addAll(Arrays.asList())
			if(!allowedKinds.contains(designatorKind))
			{
				reportError("Neodgovarajuci tip u izrazu!", designatorStatement);
				return;
			}
			
			DesignatorStatementOptions_AssignOpExpr optionsChild = (DesignatorStatementOptions_AssignOpExpr) options;
			Expr expr = optionsChild.getExpr();
			
			if(designator.obj.getType() != expr.struct)
			{
				reportError("Neodgovarajuci tip u izrazu!", designatorStatement);
				return;
			}
		}
		else if((options instanceof DesignatorStatementOptions_Inc) || (options instanceof DesignatorStatementOptions_Dec))
		{
			ArrayList<Integer> allowedKinds = new ArrayList<>(Arrays.asList(Obj.Var, Obj.Elem));
			if(!allowedKinds.contains(designatorKind))
			{
				reportError("Neodgovarajuci tip u izrazu!", designatorStatement);
				return;
			}
			
			if(designator.obj.getType() != Tab.intType)
			{
				reportError("Neodgovarajuci tip u izrazu!", designatorStatement);
				return;
			}
		}
		else if(options instanceof DesignatorStatementOptions_ActPars) // POZIV FUNKCIJE
		{
			if(designatorKind != Obj.Meth)
			{
				reportError("Neodgovarajuci tip u izrazu!", designatorStatement);
				return;
			}			
				
			Obj methodToBeCalled = designator.obj;
			
			if(this.args.size() != methodToBeCalled.getLevel())
			{
				reportError("Razlicit broj argumenata metode i poziva metode!", designatorStatement);
				return;
			}
			
			// dohvatamo prvih getLevel (broj parametara) lokalnih simbola -> sve parametre
			List<Obj> params = methodToBeCalled.getLocalSymbols()
					.stream().limit(methodToBeCalled.getLevel()).toList();
			
			for(int i = 0; i < params.size(); ++i)
			{
				Struct argType = args.get(i);
				Struct paramType = params.get(i).getType();
				
				if(argType != paramType)
				{
					reportError("Parametar " + i + " je nekompatabilan sa argumentom funkcije", designatorStatement);
				}
			}

			this.args.clear();
			log.info("Poziv funkcije " + methodToBeCalled.getName());
			
		}
		
		
	}
	
	// CONDITION
	
	public void visit(CondFact condFact)
	{
		Expr expr1 = condFact.getExpr();
		ExprRelopOptional opt = condFact.getExprRelopOptional();
		
		if(opt instanceof ExprRelopOptional_Define)
		{
			ExprRelopOptional_Define def = (ExprRelopOptional_Define) opt;
			Expr expr2 = def.getExpr();
			
			if(expr1.struct.getKind() != expr2.struct.getKind())
			{
				reportError("Izrazi nisu kompatibilni", condFact);
				return;
			}
			
			if(expr1.struct.getKind() == Struct.Array)
			{
				Relop relop = def.getRelop();
				if((relop instanceof Relop_Double_Equal) || (relop instanceof Relop_Not_Equal))
				{
					return;
				}
				
				reportError("Nevalidni relacioni operator za zadati tip! ", condFact);
				return;
			}
		}
		else if(opt instanceof ExprRelopOptional_Skip)
		{
			if(expr1.struct != TabCustom.boolType)
			{
				reportError("izraz mora da bude bool!", condFact);
			}
		}
	}
	
	// Utility
	
	public boolean passed() 
	{
		return !errorDetected;
	}
	
	private String getNamespacePrefix()
	{
		return (this.currentNamespace != "") ? (this.currentNamespace + "::") : "";
	}
}

