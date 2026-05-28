package br.com.criptovision.repository;

import br.com.criptovision.model.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Repository;

@Repository
public interface UsuarioRepository extends JpaRepository<Usuario, Long> {

    // o spring security vai usar este metodo para procurar o usuario na hora do login
    UserDetails findByLogin(String login);

}