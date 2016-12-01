#include "clang/AST/ASTConsumer.h"
#include "clang/AST/ASTContext.h"
#include "clang/AST/RecursiveASTVisitor.h"
#include "clang/Frontend/CompilerInstance.h"
#include "clang/Frontend/FrontendAction.h"
#include "clang/Tooling/CommonOptionsParser.h"
#include "clang/Tooling/Tooling.h"
#include "llvm/Support/CommandLine.h"
#include <cstdio>
#include <vector>
#include <err.h>
#include <fcntl.h>
#include <unistd.h>

#define	TRY_TO(__call)				\
	do {					\
		if (!getDerived().__call)	\
			return false;		\
	} while (/*CONSTCOND*/false)

using namespace clang;
using namespace clang::tooling;
using namespace llvm;

// Apply a custom category to all command-line options so that they are the
// only ones displayed.
static cl::OptionCategory decledCategory("decled options");

// CommonOptionsParser declares HelpMessage with a description of the common
// command-line options related to the compilation database and input files.
// It's nice to have this help message in all tools.
static cl::extrahelp CommonHelp(CommonOptionsParser::HelpMessage);

// A help message for this specific tool can be added afterwards.
static cl::extrahelp MoreHelp("\nMore help text...");

class DeclEditorVisitor : public RecursiveASTVisitor<DeclEditorVisitor> {
public:
	explicit DeclEditorVisitor(ASTContext *context,
	    llvm::StringRef in_file) : context(context) {
		this->in_file = in_file;
	}

	int operator+(int x) {
		return x;
	}
	bool visitFunctionHelper(FunctionDecl *d) {
		printf("%s\n", d->getQualifiedNameAsString().c_str());

		auto &mgr = context->getSourceManager();
		auto range = d->getNameInfo().getSourceRange();
#if 0
		auto begin = context->getFullLoc(range.getBegin());
		auto end = context->getFullLoc(range.getEnd());
#else
		auto begin = mgr.getSpellingLoc(range.getBegin());
		auto end = mgr.getSpellingLoc(range.getEnd());
#endif
		printf("\t%u.%u - %u.%u\n",
		    mgr.getSpellingLineNumber(begin),
		    mgr.getSpellingColumnNumber(begin),
		    mgr.getSpellingLineNumber(end),
		    mgr.getSpellingColumnNumber(end));

		return true;
	}

	bool VisitFunctionDecl(FunctionDecl *d) {
		return visitFunctionHelper(d);
	}

	bool VisitCXXMethodDecl(CXXMethodDecl *d) {
		return visitFunctionHelper(d);
	}

	bool VisitCXXDestructorDecl(CXXDestructorDecl *d) {
		return visitFunctionHelper(d);
	}

	bool VisitCXXConstructorDecl(CXXConstructorDecl *d) {
		return visitFunctionHelper(d);
	}

	bool VisitCXXConversionDecl(CXXConversionDecl *d) {
		return visitFunctionHelper(d);
	}
private:
	ASTContext *context;
	llvm::StringRef in_file;
};

class DeclEditorConsumer : public clang::ASTConsumer {
public:
	explicit DeclEditorConsumer(ASTContext *context,
	    llvm::StringRef in_file) : visitor(context, in_file) {}

	virtual void HandleTranslationUnit(clang::ASTContext &context) {
		visitor.TraverseDecl(context.getTranslationUnitDecl());
	}
private:
	DeclEditorVisitor visitor;
};

class DeclEditorAction : public clang::ASTFrontendAction {
public:
	virtual std::unique_ptr<clang::ASTConsumer> CreateASTConsumer(
	    clang::CompilerInstance &compiler, llvm::StringRef in_file) {
		return std::unique_ptr<clang::ASTConsumer>(
		    new DeclEditorConsumer(&compiler.getASTContext(), in_file));
	}
};

int
main(int argc, const char **argv)
{
	CommonOptionsParser parser(argc, argv, decledCategory);
	ClangTool tool(parser.getCompilations(), parser.getSourcePathList());

	return tool.run(newFrontendActionFactory<DeclEditorAction>().get());
}
