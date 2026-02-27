import java.util.HashMap;
import java.util.Map;

public class TypeSystem {
    private final Map<String, CompShType> symbolTable = new HashMap<>();

    public void addSymbol(String id, CompShType type) {
        symbolTable.put(id, type);
    }

    public CompShType getSymbolType(String id) {
        return symbolTable.getOrDefault(id, CompShType.ERROR);
    }

    public boolean symbolExists(String id) {
        return symbolTable.containsKey(id);
    }

    public CompShType checkBinaryOperation(CompShType left, String op, CompShType right) {
        switch (op) {
            case "+":
                if (left == CompShType.TEXT || right == CompShType.TEXT)
                    return CompShType.TEXT;
                // continua para casos numéricos
            case "-":
            case "*":
            case "/":
                if (left == CompShType.INTEGER && right == CompShType.INTEGER)
                    return CompShType.INTEGER;
                if ((left == CompShType.INTEGER || left == CompShType.REAL) &&
                    (right == CompShType.INTEGER || right == CompShType.REAL))
                    return CompShType.REAL;
        }
        return CompShType.ERROR;
    }

    public boolean isValidAssignment(CompShType target, CompShType source) {
        // Permite atribuição direta para tipos iguais
        if (target == source) return true;
        
        // Casos especiais:
        return (target == CompShType.PROGRAM && source == CompShType.TEXT) ||
            (target == CompShType.TEXT && (source == CompShType.INTEGER || source == CompShType.REAL)) ||
            (target == CompShType.LIST && source == CompShType.LIST);
    }

    public boolean isValidConversion(CompShType target, CompShType source) {
        if (target == source) return true;

        // Conversões permitidas (a validação do conteúdo será em tempo de execução)
        return (target == CompShType.INTEGER && source == CompShType.TEXT) ||
            (target == CompShType.REAL && source == CompShType.TEXT) ||
            (target == CompShType.TEXT && (source == CompShType.INTEGER || source == CompShType.REAL)) ||
            (target == CompShType.LIST && source == CompShType.TEXT);
    }

    public void dumpSymbolTable() {
        System.out.println("Tabela de Símbolos:");
        for (var entry : symbolTable.entrySet()) {
            System.out.printf(" - %s: %s%n", entry.getKey(), entry.getValue());
        }
    }
}