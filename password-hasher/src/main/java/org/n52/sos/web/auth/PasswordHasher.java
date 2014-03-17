package org.n52.sos.web.auth;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

public class PasswordHasher {
    public static String hashPassword(String password){
        return new BCryptPasswordEncoder().encode(password);
    }

    public static void main(String[] args){
        if (args.length != 1){
            System.err.println("Pass a single argument: the password to be hashed.");
        }
        System.out.println(hashPassword(args[0]));
    }
}
