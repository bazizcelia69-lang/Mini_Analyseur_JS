/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */

/**
 *
 * @author pc
 */
import java.util.*;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;


class AnalyseLexicalJS {

    
    static String[] jsKeywords = {
        "break", "case", "catch", "class", "const", "continue", "debugger", "default",
        "delete", "do", "else", "export", "extends", "finally", "for", "function", "if",
        "import", "in", "instanceof", "let", "new", "return", "super", "switch",
        "throw", "try", "typeof", "var", "void", "while", "with", "yield", "true", "false",
        "Baziz", "Celia" // Ajout de Baziz et Celia
    };
    
    static String[] operators = {
        "===", "!==", "==", "!=", "<=", ">=", "&&", "||", "++", "--", "**",
        "+=", "-=", "*=", "/=", "=", "<", ">", "!", "+", "-", "*", "/",
        "%", "?", ".", "=>", "...", ":", "&", "|", "^", "~", "<<", ">>", ">>>"
    };
    static String[] ponctuation = {"{", "}", "(", ")", ";", ",", "[", "]"};
    
    
    static boolean contientExact(String mot, String[] liste) {
        for (String x : liste) {
            if (x.equals(mot)) {
                return true;
            }
        }
        return false;
    }
    
    static String type(String lex) {
        
        if (contientExact(lex, jsKeywords)) {
            return "MotCle";
        }
        if (lex.equals("this")) {
            return "This";
        }
 
        // Les opérateurs et les ponctuations 
        if (contientExact(lex, operators) || contientExact(lex, ponctuation)) {
             if (contientExact(lex, operators)) return "Operateur";
             if (contientExact(lex, ponctuation)) return "Ponctuation";
        }
        
        // Identificateurs
        if (lex.matches("[a-zA-Z_$][a-zA-Z0-9_$]*")) {
            return "Identificateur";
        }
        // Nombres (entiers ou décimaux)
        if (lex.matches("[0-9]+(\\.[0-9]+)?")) {
            return "Nombre";
        }
        return "ERREUR";
    }

    
    static Vector<String[]> analyse(String code, Vector<String[]> erreurs) {
        Vector<String[]> tokens = new Vector<>();
        int i = 0, ligne = 1;
        
        while (code.charAt(i) != '#') {
            char c = code.charAt(i);
            
            if (c == '\n') {
                ligne++;
                i++;
                continue;
            }
            
            if (Character.isWhitespace(c)) {
                i++;
                continue;
            }
            
            // Gestion des commentaires 
            if (c == '/') {
                // Vérifie si le fichier n'est pas terminé
                if (i + 1 >= code.length() || code.charAt(i + 1) == '#') {
                    // C'est juste un opérateur '/' non suivi d'un second charactère significatif (géré plus tard)
                } else {
                    char nextC = code.charAt(i + 1);

                    if (nextC == '/') { // Commentaire //
                        i += 2;
                        while (code.charAt(i) != '\n' && code.charAt(i) != '#') {
                            i++;
                        }
                        continue;
                    }
                    
                    if (nextC == '*') { // Commentaire /* */
                        int startLigne = ligne;
                        i += 2;
                        boolean commentaireFerme = false;
                        while (code.charAt(i) != '#') {
                            if (code.charAt(i) == '*' && code.charAt(i + 1) == '/') {
                                i += 2;
                                commentaireFerme = true;
                                break;
                            }
                            if (code.charAt(i) == '\n') {
                                ligne++;
                            }
                            i++;
                        }
                        if (!commentaireFerme) {
                             erreurs.add(new String[]{"/*", "ERREUR (Commentaire non termine)", String.valueOf(startLigne)});
                             tokens.add(new String[]{"/*", "ERREUR", String.valueOf(startLigne)});
                        }
                        continue;
                    }
                }
            }
            
            //  Gestion des operateurs et ponctuations ( les operateurs de longueur 1,2,3)
            //  Priorité au plus long lexème (ex: '===' au lieu de '=')
            String bestMatch = "";
            int maxLength = 0;

            
            for (int len = 3; len >= 1; len--) {
                if (i + len <= code.length() && code.charAt(i + len - 1) != '#') {
                    String sub = code.substring(i, i + len);
                    if (contientExact(sub, operators) || contientExact(sub, ponctuation)) {
                         if (sub.length() > maxLength) {
                             maxLength = sub.length();
                             bestMatch = sub;
                         }
                    }
                }
            }

            if (maxLength > 0) {
                String foundType = type(bestMatch);
                tokens.add(new String[]{bestMatch, foundType, String.valueOf(ligne)});
                i += maxLength;
                continue;
            }
            

            // Gestion des chaines de caracteres (Strings)
            if (c == '"' || c == '\'') {
                char quote = c;
                int start = i;
                int startLigne = ligne;
                i++;
                boolean closed = false;
                while (code.charAt(i) != '#') {
                    if (code.charAt(i) == quote) {
                        closed = true;
                        i++;
                        break;
                    }
                    if (code.charAt(i) == '\n') {
                        break; // Les strings ne doivent pas contenir de saut de ligne non échappé
                    }
                    if (code.charAt(i) == '\\' && code.charAt(i + 1) != '#') {
                        i++; // Gère l'échappement (ex: \n, \", \\)
                    }
                    i++;
                }
                
                String lexeme = code.substring(start, i);
                if (closed) {
                    // La vérification contientIgnoreMN est retirée ici car elle concernait motsPerso
                    tokens.add(new String[]{lexeme, "String", String.valueOf(startLigne)}); 
                } else {
                    erreurs.add(new String[]{lexeme, "ERREUR (String non fermee)", String.valueOf(startLigne)});
                    tokens.add(new String[]{lexeme, "ERREUR", String.valueOf(startLigne)});
                    // Si on a atteint la fin du fichier (#) ou un saut de ligne, 'i' est déjà incrémenté.
                }
                continue;
            }

            // Gestion des identificateurs (noms de variables/fonctions)
            if (Character.isLetter(c) || c == '_' || c == '$') {
                int start = i;
                int startLigne = ligne;
                i++;
                while (Character.isLetterOrDigit(code.charAt(i)) || code.charAt(i) == '_' || code.charAt(i) == '$') {
                    i++;
                }
                String word = code.substring(start, i);
                tokens.add(new String[]{word, type(word), String.valueOf(startLigne)});
                continue;
            }

            // Gestion des nombres
            if (Character.isDigit(c)) {
                int start = i;
                int startLigne = ligne;
                i++;
                boolean pointDecimalVu = false;
                boolean isError = false;
                
                while (code.charAt(i) != '#') {
                    char x = code.charAt(i);
                    if (Character.isDigit(x)) {
                        i++;
                        continue;
                    }
                    if (x == '.') {
                        if (pointDecimalVu) break; // Deux points décimaux
                        pointDecimalVu = true;
                        i++;
                        continue;
                    }
                    if (Character.isLetter(x) || x == '_' || x == '$') {
                        // Erreur: nombre suivi de lettre 
                        i++;
                        while (Character.isLetterOrDigit(code.charAt(i)) || code.charAt(i) == '_' || code.charAt(i) == '$') i++;
                        
                        String lexErreur = code.substring(start, i);
                        erreurs.add(new String[]{lexErreur, "ERREUR (nombre suivi de lettre)", String.valueOf(startLigne)});
                        tokens.add(new String[]{lexErreur, "ERREUR", String.valueOf(startLigne)});
                        isError = true;
                        break; //Sort de la boucle 
                    }
                    break; // Fin du nombre (espaces, ; ou autre)
                }
                
                if (!isError) {
                    String lex = code.substring(start, i);
                    tokens.add(new String[]{lex, "Nombre", String.valueOf(startLigne)});
                }
                continue;
            }

            // Caractère inconnu (si non capturé par les opérateurs de 1 à 3 caractères plus haut)
            tokens.add(new String[]{"" + c, "ERREUR", String.valueOf(ligne)});
            erreurs.add(new String[]{"" + c, "ERREUR (Caractere inconnu)", String.valueOf(ligne)});
            i++;
        }
        
        tokens.add(new String[]{"#", "EOF", String.valueOf(ligne)});
        return tokens;
    }
}


class AnalyseSyntaxiqueJS {

    Vector<String[]> tokens;
    int j;    
    boolean ok = true; // Pour letat final (succes/echec)
    boolean dansClasse = false;

    
    Set<String> statementStarters = new HashSet<>(Arrays.asList(
        "let", "const", "var", "function", "class", "if", "while", "for", "switch", "return", "}"
    ));

    // recupere le lexme
    String lex() {
        return j >= tokens.size() ? "#" : tokens.get(j)[0];
    }

    // recupere le type
    String type() {
        return j >= tokens.size() ? "EOF" : tokens.get(j)[1];
    }
    
    // recupere la ligne
    String ligne() {
        // Retourne la ligne du jeton courant, ou celle du dernier jeton si on est à la fin.
        if (j >= tokens.size()) {
            return tokens.isEmpty() ? "1" : tokens.get(tokens.size() - 1)[2];
        }
        return tokens.get(j)[2];
    }

    
    void erreur(String msg) {
        
        String messageComplet = "Erreur syntaxique ligne " + ligne() + " : " + msg + " pres de '" + lex() + "'";
        
        if (ok) {
            System.out.println("\n ERREURS SYNTAXIQUES DETECTEES ");
            System.out.println(messageComplet);
        } else {
            System.out.println(messageComplet);
        }
        
        ok = false;
    }
    
    
    void programme() {
        while (!lex().equals("#")) {    
            instruction();
        }
    }

    // Gère le début de chaque type d'instruction
    void instruction() {
        // Cas des mots cle qui introduisent des blocs complets
        if (lex().equals("function")) { fonctionGlobale(); return; }
        if (lex().equals("class")) { classe(); return; }
        if (lex().equals("switch")) { switchCase(); return; }
        
        if (lex().equals("return")) {
            j++;    
            if (!lex().equals(";")) {
                expression();
            }
            if (!lex().equals(";")) {
                erreur("; attendu apres return");
                //saute jusqu au prochain ; ou }
                while (!lex().equals(";") && !lex().equals("}") && !lex().equals("#")) j++;
            }
            if (lex().equals(";")) j++;    
            return;
        }
        
        if (lex().equals("let") || lex().equals("const") || lex().equals("var")) {    
            declarationLet();
            return;
        }
        
        //Gestion des blocs {} 
        if (lex().equals("{")) {
            j++;    
            int oldJ = j;
            while (!lex().equals("}") && !lex().equals("#") && j < tokens.size()) {
                instruction();
                // Avance pour éviter une boucle infinie si instruction() ne consomme pas de jeton (en cas d'erreur de récupération)
                if(oldJ == j) j++; 
                oldJ = j;
            }
            
            if (lex().equals("#")) {
                erreur("} manquant pour fermer le bloc d'instructions ");
            }
            
            if (lex().equals("}")) j++;    
            return;
        }

        // Instructions de controle (if, while, etc.)
        if (lex().equals("if") || lex().equals("else") || lex().equals("while")
             || lex().equals("for") || lex().equals("do") || lex().equals("try")
             || lex().equals("catch") || lex().equals("throw") || lex().equals("finally")) {
            
            String motCle = lex();
            j++;    
            
            // Si cest  'while' 'if' ou 'for' on gere les parentheses
            if (motCle.equals("if") || motCle.equals("while") || motCle.equals("for")) {
                if (!lex().equals("(")) {
                    erreur("( attendu apres " + motCle);
                } else {
                    j++;    
                    expression();    
                    if (!lex().equals(")")) {
                        erreur(") attendu pour fermer la condition " + motCle);
                        while (!lex().equals(";") && !lex().equals("{") && !lex().equals("#")) j++; 
                    } else {
                        j++;
                    }
                }
            }
            
            // L'instruction qui suit (le corps)
            instruction();
            
            
            if (motCle.equals("if") && lex().equals("else")) {
                j++;
                instruction();
            }
            
            return;
        }

        // Expression/Affectation
        if (type().equals("Identificateur") || type().equals("This") || type().equals("Nombre") || lex().equals("new") || lex().equals("(") || type().equals("String") || type().equals("ERREUR") || type().equals("MotCle")) {
            
            expression();    
            
            if (lex().equals("=") || lex().equals("+=") || lex().equals("-=") || lex().equals("*=") || lex().equals("/=")) {    
                j++;    
                expression();
            }
            
            if (lex().equals(";")) {
                j++;    
            } else if (!lex().equals("}")) { 
                
                erreur("; attendu apres expression ou affectation");
                
                while (!lex().equals(";") && !lex().equals("}") && !lex().equals("#") 
                         && !statementStarters.contains(lex())) {
                             j++;
                }
                if (lex().equals(";")) j++;
            }
            return;
        }
        
        // Instruction inconnue
        if (!lex().equals("#")) {
            erreur("Instruction inconnue ou jeton inattendu");
            
            //gestion de recuperation apres lerreur
            while (!lex().equals(";") && !lex().equals("}") && !lex().equals("#") 
                       && !statementStarters.contains(lex())) {
                       j++; 
            }
            
            if (lex().equals(";")) j++; 
        }
    }
    
    
    void appelFonction(boolean pointVirguleObligatoire) {
        
        if (!lex().equals("(")) {
            return; 
        }
        j++; 

        // Gère le cas sans arguments f()
        if (lex().equals(")")) {
             j++; 
        } else {
            // Gère le cas avec un ou plusieurs arguments
            do {
                expression();
            } while (lex().equals(",") && j++ > 0); 
        
            // Après la boucle des arguments, on DOIT trouver ')'
            if (!lex().equals(")")) {
                erreur(") attendu apres les arguments de fonction");
                // essayer de continuer apres l'erreur
                while (!lex().equals(";") && !lex().equals("}") && !lex().equals("#") && !lex().equals(",")) j++;
            }
            // Consomme ) si trouvé
            if (lex().equals(")")) j++;
        }

        if (pointVirguleObligatoire) {
            if (!lex().equals(";")) erreur("; attendu après appel de fonction");
            else j++;    
        }
    }


    void classe() {
        j++; 
        if (!type().equals("Identificateur")) {
            erreur("Nom de classe attendu");
        } else {
            j++;    
        }
        
        // Gestion de lheritage (extends)
        if (lex().equals("extends")) {
             j++;
             if (!type().equals("Identificateur")) {
                 erreur("Nom de classe parente attendu apres 'extends'");
             } else {
                 j++;
             }
        }
        
        if (!lex().equals("{")) {
            erreur("{ attendu");
            // recuperer apres lerreur
            while (!lex().equals("{") && !lex().equals("#")) j++;
            if (lex().equals("{")) j++;
        } else {
            j++;    
        }

        boolean ancienDansClasse = dansClasse;
        dansClasse = true;

        while (!lex().equals("}") && !lex().equals("#")) {
            // Les attributs et methodes sont analysés ici
            if (type().equals("Identificateur")) {
                String nom = lex();    
                j++;    
                
                if (lex().equals("(")) { // Methode
                    
                    appelFonction(false); 
                    
                    if (!lex().equals("{")) erreur("{ attendu pour le corps de la methode " + nom);
                    else j++;    

                    while (!lex().equals("}") && !lex().equals("#")) {
                        instruction();
                    }

                    if (!lex().equals("}")) erreur("} manquant pour fermer la methode " + nom);
                    else j++;    
                    
                    continue;    
                }

                if (lex().equals("=")) { // Attribut avec affectation
                    j++;    
                    expression();
                }
                
                if (!lex().equals(";")) {
                    erreur("; attendu apres declaration/affectation d'attribut " + nom);
                    // recuperation apres erreur
                    while (!lex().equals(";") && !lex().equals("}") && !lex().equals("#")) j++;
                }
                if (lex().equals(";")) j++;    
                
                continue;
            }

            // recuperation dans la classd
            erreur("Declaration invalide dans la classe (identificateur attendu)");
            while (!lex().equals(";") && !lex().equals("}") && !lex().equals("#")) {
                j++;
            }
            if (lex().equals(";")) j++; 
        }

        if (lex().equals("#")) {
            erreur("} manquant pour fermer la classe (Fin de fichier)");
        }
        
        if (lex().equals("}")) {
            j++;    
        }

        dansClasse = ancienDansClasse;
    }

    void fonctionGlobale() {
        j++; 

        if (!type().equals("Identificateur")) {
            erreur("Nom de fonction attendu apres 'function'");
            while (!lex().equals(";") && !lex().equals("{") && !lex().equals("#")) j++;
            return;
        }
        j++;    

        // parametres ( )
        appelFonction(false); 

        // Corps { ... }
        if (!lex().equals("{")) erreur("{ attendu pour le corps de la fonction");
        else j++;    

        while (!lex().equals("}") && !lex().equals("#")) {
            instruction();    
        }

        if (lex().equals("#")) {
            erreur("} manquant pour fermer la fonction (Fin de fichier)");
        }
        
        if (lex().equals("}")) {
            j++;    
        }
    }

    
    void declarationLet() {
        j++; 

        do {
            if (lex().equals(",")) j++;    
            
            // Gere l'erreur lexicale en la consommant
            if (type().equals("ERREUR")) {
                erreur("Identificateur invalide (erreur lexicale: " + lex() + ")");
                j++; // Consomme lerreur
                continue;    
            }
            
            
            if (!type().equals("Identificateur")) {
                erreur("Identificateur attendu apres mot-cle de declaration ou ','");
                j++;    
                continue;    
            }
            
            // Cas valide
            j++; 

            if (lex().equals("=")) {
                j++;    
                expression();
            }
        } while (lex().equals(","));


        if (!lex().equals(";")) {
            erreur("; attendu apres declaration let/const/var");
            
            while (!lex().equals(";") && !lex().equals("}") && !lex().equals("#") 
                        && !statementStarters.contains(lex())) {
                             j++;
            }
        }
        if (lex().equals(";")) j++;    
    }

    void expression() {
        
        // operateur unaire
        if (type().equals("Operateur") || lex().equals("typeof")) {
            if (lex().equals("+") || lex().equals("-") || lex().equals("!") || lex().equals("++") || lex().equals("--") || lex().equals("typeof")) {
                j++;    
                expression();    
                return;
            }
        }
        
        // Parentheses dans l'expression 
        if (lex().equals("(")) {
            j++;    
            expression();
            if (!lex().equals(")")) {
                erreur(") attendu pour fermer l'expression");
                while (!lex().equals(";") && !lex().equals(")") && !lex().equals("}") && !lex().equals("#")) j++;
            }
            if (lex().equals(")")) j++;    
        }    
        // Creation dinstance (new)
        else if (lex().equals("new")) {
            j++;    
            if (!type().equals("Identificateur")) {
                erreur("Nom de classe attendu apres 'new'");
            } else {
                j++;    
            }
            
            // Appel du constructeur () o^ptionnel
            if (lex().equals("(")) {
                appelFonction(false); 
            }
        }
        // (Identificateur, Nombre, String, This, etc), maintenant MotCle inclus
        else if (type().equals("Identificateur") || type().equals("This") || type().equals("Nombre") || type().equals("String") || type().equals("MotCle") || type().equals("ERREUR")) {
            j++;    
        }    
        else {
            return;
        }

        // Acces a la proprieté, appel de methode, operateurs post fixes
        while (true) {
            if (lex().equals(".")) {
                j++;    
                if (!type().equals("Identificateur")) {
                    erreur("Identificateur attendu apres '.'");
                } else {
                    j++;    
                }
            } else if (lex().equals("(")) {
                appelFonction(false);    
            } else if (type().equals("Operateur") && (lex().equals("++") || lex().equals("--"))) {
                j++;    
            }
            else {
                break;
            }
        }
        
        // Gestion des operateurs binaires et ternaires
        while (type().equals("Operateur") && !lex().equals(";")) {
            String op = lex();
            
            // Cas du ternaire 
            if (op.equals("?")) {    
                j++; 
                expression(); 
                if (!lex().equals(":")) {
                    erreur(": attendu pour ternaire");
                    while (!lex().equals(";") && !lex().equals("#")) j++;
                } else {
                    j++; 
                }
                expression(); 
            } 
            
            // Cas des operateurs binaires standards (+, -, *, ==, <, etc)
            else {
                j++;    
                expression(); // L'operande de droite
            }
        }
    }

    void switchCase() {
        j++; 
        if (!lex().equals("(")) {
            erreur("( attendu apres switch");
        } else {
            j++;    
        }
        expression();
        if (!lex().equals(")")) {
            erreur(") attendu pour la condition switch");
        } else {
            j++;    
        }
        if (!lex().equals("{")) {
            erreur("{ attendu pour le corps switch");
        } else {
            j++;    
        }
        
        // Boucle d'analyse des blocs case
        while (!lex().equals("}") && !lex().equals("#")) {
            caseBlock();
        }
        
        if (lex().equals("#")) {
            erreur("} manquant pour fermer le switch (Fin de fichier)");
        }
        
        if (lex().equals("}")) {
            j++;    
        }
    }

    void caseBlock() {
        if (!lex().equals("case") && !lex().equals("default")) {
            erreur("case ou default attendu");
            while (!lex().equals("case") && !lex().equals("default") && !lex().equals("}") && !lex().equals("#")) {
                j++;    
            }
            return;
        }
        
        boolean isDefault = lex().equals("default");
        j++;    
        
        if (!isDefault) {
            // "MotClePerso" remplacé par "MotCle"
            if (!(type().equals("Nombre") || type().equals("Identificateur") || type().equals("String") || type().equals("MotCle") || type().equals("ERREUR"))) {
                erreur("Valeur invalide case (litteral ou identificateur attendu)");
                while (!lex().equals(":") && !lex().equals("#")) j++;
            } else {
                j++;
            }
        }
        
        if (!lex().equals(":")) {
            erreur(": attendu apres case/default");
            while (!lex().equals(":") && !lex().equals("#")) j++;
        } 
        if (lex().equals(":")) {
             j++;    
        }
        
        // Instructions dans le bloc case
        while (!lex().equals("break") && !lex().equals("case") && !lex().equals("default") && !lex().equals("}") && !lex().equals("#")) {
            instruction();
        }
        
        if (lex().equals("break")) {
            j++;    
            if (!lex().equals(";")) {
                erreur("; attendu apres break");
                while (!lex().equals(";") && !lex().equals("#")) j++;
            }
            if (lex().equals(";")) j++;    
        }
    }

    boolean analyser(Vector<String[]> t) {
        tokens = t;
        j = 0;
        ok = true;
        programme();
        return ok;
    }

}

public class AnalyseurJSs {

    public static void main(String[] args) {
        Scanner scConsole = new Scanner(System.in);
        
        while (true) {
            System.out.println("Veuillez entrer le chemin complet du fichier JavaScript (tapez 'exit' pour quitter) :");
            
            String filePath = scConsole.nextLine().trim();
            
            if (filePath.equalsIgnoreCase("exit")) {
                System.out.println("Fin de l'analyseur.");
                break;
            }

            File file = new File(filePath);
            String code = "";
            boolean fileReadSuccess = false;

            try {
                List<String> allLines = Files.readAllLines(Paths.get(filePath));
                code = String.join("\n", allLines).trim();
                
                if (code.isEmpty() && !allLines.isEmpty()) {
                    System.out.println("Le fichier est vide ou ne contient que des espaces/lignes vides.");
                    continue;
                }
                
                fileReadSuccess = true;

            } catch (FileNotFoundException e) {
                 System.out.println("ERREUR: Le fichier '" + filePath + "' n'a pas ete trouve. Veuillez verifier le chemin.");
                 continue; // Recommence la boucle
            } catch (IOException e) {
                System.out.println("ERREUR de lecture du fichier: " + e.getMessage());
                continue;
            }

            if (fileReadSuccess) {
                code += "#"; 

                Vector<String[]> erreursLexicales = new Vector<>();
                Vector<String[]> tok = AnalyseLexicalJS.analyse(code, erreursLexicales);

                System.out.println("\nLEXEMES : ");
              
                for (int k = 0; k < tok.size() -1; k++) {    
                    String[] t = tok.get(k);
                    // Affichage 
                    System.out.println(t[0] + " -> " + t[1] + " (ligne " + t[2] + ")");    
                }
                
                if (!erreursLexicales.isEmpty()) {
                    System.out.println("\nERREURS LEXICALES DETECTEES : ");
                    for (String[] e : erreursLexicales) {
                        System.out.println("Erreur : '" + e[0] + "' type: " + e[1] + " ligne " + e[2]);
                    }
                }

                AnalyseSyntaxiqueJS syn = new AnalyseSyntaxiqueJS();
                boolean ok = syn.analyser(tok);    
                
                System.out.println("\n RESULTAT SYNTAXIQUE : ");
                if (ok) {
                    System.out.println("Analyse syntaxique reussie. ");
                } else {
                    System.out.println(" Analyse syntaxique echouee. ");
                }
            }
        }
        scConsole.close();
    }
}