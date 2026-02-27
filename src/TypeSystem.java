import java.util.HashMap;
import java.util.Map;

public class TypeSystem {
    private final Map<String, Type> symbolTable = new HashMap<>();

    public void addSymbol(String id, Type type) {
        symbolTable.put(id, type);
    }

    public Type getSymbolType(String id) {
        return symbolTable.getOrDefault(id, Type.ERROR);
    }

    public boolean symbolExists(String id) {
        return symbolTable.containsKey(id);
    }

    public Type checkBinaryOperation(Type left, String op, Type right) {
        switch (op) {
            case "+":
                if (left == Type.TEXT || right == Type.TEXT)
                    return Type.TEXT;
                // continua para casos numéricos
            case "-":
            case "*":
            case "/":
                if (left == Type.INTEGER && right == Type.INTEGER)
                    return Type.INTEGER;
                if ((left == Type.INTEGER || left == Type.REAL) &&
                    (right == Type.INTEGER || right == Type.REAL))
                    return Type.REAL;
        }
        return Type.ERROR;
    }

    public boolean isValidAssignment(Type target, Type source) {
        // Permite atribuição direta para tipos iguais
        if (target == source) return true;
        
        // Casos especiais:
        return (target == Type.PROGRAM && source == Type.TEXT) ||
            (target == Type.TEXT && (source == Type.INTEGER || source == Type.REAL)) ||
            (target == Type.LIST && source == Type.LIST);
    }

    public boolean isValidConversion(Type target, Type source) {
        if (target == source) return true;

        // Conversões permitidas (a validação do conteúdo será em tempo de execução)
        return (target == Type.INTEGER && source == Type.TEXT) ||
            (target == Type.REAL && source == Type.TEXT) ||
            (target == Type.TEXT && (source == Type.INTEGER || source == Type.REAL)) ||
            (target == Type.LIST && source == Type.TEXT); 
    }

    public void dumpSymbolTable() {
        System.out.println("Tabela de Símbolos:");
        for (var entry : symbolTable.entrySet()) {
            System.out.printf(" - %s: %s%n", entry.getKey(), entry.getValue());
        }
    }
}