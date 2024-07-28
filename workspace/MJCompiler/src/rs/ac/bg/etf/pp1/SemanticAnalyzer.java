package rs.ac.bg.etf.pp1;
import org.apache.log4j.Logger;

import rs.ac.bg.etf.pp1.ast.*;
import rs.etf.pp1.symboltable.*;
import rs.etf.pp1.symboltable.concepts.*;

public class SemanticAnalyzer extends VisitorAdaptor {

	private boolean errorDetected = false;
	private int printCallCount = 0;
	private int nVars;
	private boolean returnFound = false;
	
	private Type currentType;
	private Obj currentMethod = null;
	private String currentNamespace = "";

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
		
		// provera da li namesapce nije dobrog tipa?
		
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
		
		
		nVars = Tab.currentScope.getnVars();
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
		Tab.chainLocalSymbols(namespace.getNamespaceName().obj);
		Tab.closeScope();
		
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
		typeAndName.obj = currentMethod;
		
		Tab.openScope();
	}
	
	@Override
	public void visit(MethodDeclarationTypeAndName_Void typeAndName)
	{
		String methodName = this.getNamespacePrefix() + typeAndName.getMethodName();
		if(Tab.currentScope.findSymbol(methodName) != null) // metoda vec definisana u scope-u
		{
			reportError("Redefinicija metode " + methodName, typeAndName);
		}
		
		log.info("Obrada metode " + methodName);
		currentMethod = Tab.insert(Obj.Meth, methodName, Tab.noType);
		typeAndName.obj = currentMethod;
		
		Tab.openScope();
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

