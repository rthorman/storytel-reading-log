package com.readinglog.storytel;

public final class LogRulesTest {
    public static void main(String[] args) {
        LogRules.validate("Book","Author","","");
        LogRules.validate("Book","Author","2025-01-01","2025-01-01");
        rejects("","Author","","");
        rejects("Book","Author","2025-01-03","2025-01-02");
        rejects("Book","Author","","2025-01-02");
        rejects("Book","Author","2025-02-30","");
        rejects("Book","Author","2999-01-01","");
        if(!LogRules.status("","").equals("To read")) throw new AssertionError();
        if(!LogRules.status("2025-01-01","").equals("In progress")) throw new AssertionError();
        if(!LogRules.status("2025-01-01","2025-01-02").equals("Finished")) throw new AssertionError();
        System.out.println("Reading log validation checks passed");
    }
    private static void rejects(String t,String a,String s,String f) {
        try { LogRules.validate(t,a,s,f); } catch(IllegalArgumentException expected) { return; }
        throw new AssertionError("Expected invalid reading dates or missing title to be rejected");
    }
}
