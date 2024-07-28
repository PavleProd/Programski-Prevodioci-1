package rs.ac.bg.etf.pp1;
import org.apache.log4j.Logger;

import rs.ac.bg.etf.pp1.ast.*;
import rs.etf.pp1.symboltable.*;
import rs.etf.pp1.symboltable.concepts.*;

public class SemanticAnalyzer extends VisitorAdaptor {

	private boolean errorDetected = false;
	private int printCallCount = 0;
	private int nVars;
	
	private Type currType;
	private Obj currentMethod = null;
	private boolean returnFound = false;

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
	
	@Override
	public void visit(Type_NoScope type) 
	{
		String typeName = type.getTypeName();
		Obj typeNode = Tab.find(typeName);
		this.currType = type;
		
		if(typeNode == Tab.noObj) // nije pronadjen tip
		{
			reportError("Nije pronadjen tip " + typeName + " u tabeli simbola!", null);
			type.struct = Tab.noType;
			return;
		}
		
		if(typeNode.getKind() != Obj.Type) // nije tip (nego promenljiva, klasa itd)
		{
			reportError("Greska: Ime " + typeName + " ne predstavlja tip!", type);
			type.struct = Tab.noType;
			return;
		}
		
		log.info("Tip: " + typeName);
		type.struct = typeNode.getType();
	}
	
	@Override
	public void visit(Type_Scope type)
	{
		this.currType = type;
		
		String namespaceName = type.getNamespaceName();
		String typeName = type.getTypeName();
		String typeFullName = namespaceName + "::" + typeName;
		
		Obj namespaceNode = Tab.find(namespaceName);
		
		if(namespaceNode == Tab.noObj) // namespace ne postoji
		{
			reportError("Nije pronadjen namespace " + namespaceName + " u tabeli simbola!", type);
		}
		
		// provera da li namesapce nije dobrog tipa?
		
		Obj typeNode = Tab.find(typeFullName);
		
		if(typeNode == Tab.noObj) // tip ne postoji
		{
			reportError("Nije pronadjen tip " + typeFullName + " u tabeli simbola!", type);
			type.struct = Tab.noType;
			return;
		}
		
		if(typeNode.getKind() != Obj.Type) // nije tip (nego promenljiva, klasa itd)
		{
			reportError("Greska: Ime " + typeFullName + " ne predstavlja tip!", type);
			type.struct = Tab.noType;
			return;
		}
		
		log.info("Tip: " + typeFullName);
		type.struct = typeNode.getType();
	}
	
	@Override
	public void visit(Program program) {
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
	public void visit(ProgramName progName) 
	{
		// kind (enum simbola), name, type
		log.info("Obrada programa " + progName.getName());
		progName.obj = Tab.insert(Obj.Prog, progName.getName(), Tab.noType);
		Tab.openScope();     	
	}

	public boolean passed() {
		return !errorDetected;
	}
	
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
		String methodName = typeAndName.getMethodName();
		if(Tab.currentScope.findSymbol(methodName) != null) // metoda vec definisana u scope-u
		{
			reportError("Redefinicija metode " + methodName, null);
			return;
		}
		
		log.info("Obrada metode " + methodName);
		currentMethod = Tab.insert(Obj.Meth, typeAndName.getMethodName(), this.currType.struct);
		typeAndName.obj = currentMethod;
		
		Tab.openScope();
	}
	
	public void visit(MethodDeclarationTypeAndName_Void typeAndName)
	{
		String methodName = typeAndName.getMethodName();
		if(Tab.currentScope.findSymbol(methodName) != null) // metoda vec definisana u scope-u
		{
			reportError("Redefinicija metode " + methodName, null);
			return;
		}
		
		log.info("Obrada metode " + methodName);
		currentMethod = Tab.insert(Obj.Meth, typeAndName.getMethodName(), Tab.noType);
		typeAndName.obj = currentMethod;
		
		Tab.openScope();
	}
	
}

