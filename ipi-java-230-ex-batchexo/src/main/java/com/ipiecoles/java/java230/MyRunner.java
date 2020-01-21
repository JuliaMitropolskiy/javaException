package com.ipiecoles.java.java230;

import com.ipiecoles.java.java230.exceptions.BatchException;
import com.ipiecoles.java.java230.exceptions.TechnicienException;
import com.ipiecoles.java.java230.model.Commercial;
import com.ipiecoles.java.java230.model.Employe;
import com.ipiecoles.java.java230.model.Manager;
import com.ipiecoles.java.java230.model.Technicien;
import com.ipiecoles.java.java230.repository.EmployeRepository;
import com.ipiecoles.java.java230.repository.ManagerRepository;

import org.joda.time.LocalDate;
import org.joda.time.format.DateTimeFormat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Component
public class MyRunner implements CommandLineRunner {

    private static final String REGEX_MATRICULE = "^[MTC][0-9]{5}$";
    private static final String REGEX_NOM = ".*";
    private static final String REGEX_PRENOM = ".*";
    private static final int NB_CHAMPS_MANAGER = 5;
    private static final int NB_CHAMPS_TECHNICIEN = 7;
    private static final String REGEX_MATRICULE_MANAGER = "^M[0-9]{5}$";
    private static final int NB_CHAMPS_COMMERCIAL = 7;

    @Autowired
    private EmployeRepository employeRepository;

    @Autowired
    private ManagerRepository managerRepository;

    private List<Employe> employes = new ArrayList<Employe>();

    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    @Override
    public void run(String... strings) throws Exception {
        String fileName = "employes.csv";
        readFile(fileName);
        //employeRepository.saveAll(employes);
    }

    /**
     * Méthode qui lit le fichier CSV en paramètre afin d'intégrer son contenu en BDD
     * @param fileName Le nom du fichier (à mettre dans src/main/resources)
     * @return une liste contenant les employés à insérer en BDD ou null si le fichier n'a pas pu être lu
     */
    public List<Employe> readFile(String fileName) throws Exception {
        Stream<String> stream;
        stream = Files.lines(Paths.get(new ClassPathResource(fileName).getURI()));
        Integer i = 0;
        for (String ligne : stream.collect(Collectors.toList())) {
        	i++;
        	try {
        		processLine(ligne);
        	} catch (BatchException e) {
        		System.out.println("Ligne " + i + " : " + e.getMessage() + " => " + ligne);
        	}
        }
        return employes;
    }

    /**
     * Méthode qui regarde le premier caractère de la ligne et appelle la bonne méthode de création d'employé
     * @param ligne la ligne à analyser
     * @throws BatchException si le type d'employé n'a pas été reconnu
     */
    private void processLine(String ligne) throws BatchException {
    	if (!ligne.matches("^[MCT]{1}.*")) {
    		throw new BatchException ("Type d'employé inconnu : " + ligne.charAt(0));
    	}
    	String[] ligneDivisee = ligne.split(",");
    	String matricule = ligneDivisee[0];
    	if (!matricule.matches(REGEX_MATRICULE)) {
    		throw new BatchException ("la chaîne " + matricule + " ne respecte pas l'expression régulière " + REGEX_MATRICULE);
    	}
    	if (matricule.substring(0, 1).equals("C")) {
    		processCommercial(ligneDivisee);
    	} else if (matricule.substring(0, 1).equals("M")) {
    		processManager(ligneDivisee);
    	} else processTechnicien(ligneDivisee);
    }

    /**
     * Méthode qui crée un Commercial à partir d'une ligne contenant les informations d'un commercial et l'ajoute dans la liste globale des employés
     * @param ligneCommercial la ligne contenant les infos du commercial à intégrer
     * @throws BatchException s'il y a un problème sur cette ligne
     */
    private void processCommercial(String[] ligneCommercial) throws BatchException {
    	Commercial c = new Commercial();
        if (ligneCommercial.length != NB_CHAMPS_COMMERCIAL) {
        	throw new BatchException ("La ligne commercial ne contient pas " + NB_CHAMPS_COMMERCIAL + " éléments mais " + ligneCommercial.length);
        }
        checkCommonFields(ligneCommercial, c);
        Double caAnnuel;
        Integer performance;
        try {
        	caAnnuel = Double.parseDouble(ligneCommercial[5]);
	    } catch (Exception e) {
	    	throw new BatchException ("Le chiffre d'affaire du commercial est incorrect : " + ligneCommercial[5]);
	    }
        try {
	    	performance = Integer.parseInt(ligneCommercial[6]);
	    } catch (Exception e) {
	    	throw new BatchException ("La performance du commercial est incorrecte : " + ligneCommercial[6]);
	    }
        c.setCaAnnuel(caAnnuel);
        c.setPerformance(performance);
		employes.add(c);
    
    }

    /**
     * Méthode qui crée un Manager à partir d'une ligne contenant les informations d'un manager et l'ajoute dans la liste globale des employés
     * @param ligneManager la ligne contenant les infos du manager à intégrer
     * @throws BatchException s'il y a un problème sur cette ligne
     */
    private void processManager(String[] ligneManager) throws BatchException {
    	Manager m = new Manager();
        if (ligneManager.length != NB_CHAMPS_MANAGER) {
        	throw new BatchException("La ligne manager ne contient pas " + NB_CHAMPS_MANAGER + " éléments mais " + ligneManager.length);
        }
        checkCommonFields(ligneManager, m);
        employes.add(m);
    }

    /**
     * Méthode qui crée un Technicien à partir d'une ligne contenant les informations d'un technicien et l'ajoute dans la liste globale des employés
     * @param ligneTechnicien la ligne contenant les infos du technicien à intégrer
     * @throws BatchException s'il y a un problème sur cette ligne
     */
    private void processTechnicien(String[] ligneTechnicien) throws BatchException {
    	Technicien t = new Technicien();
    	if (ligneTechnicien.length != NB_CHAMPS_TECHNICIEN) {
        	throw new BatchException ("La ligne technicien ne contient pas " + NB_CHAMPS_TECHNICIEN + " éléments mais " + ligneTechnicien.length);
        }    
        Integer grade;
        try {
	    	grade = Integer.parseInt(ligneTechnicien[5]);
	    } catch (Exception e) {
	    	throw new BatchException ("Le grade du technicien est incorrect : " + ligneTechnicien[5]);
	    }
        if (grade < 1 || grade > 5) {
        	throw new BatchException ("Le grade doit être compris entre 1 et 5 : " + grade + ", technicien : " + t.toString());
        }
        String matriculeManager = ligneTechnicien[6];
        if (!matriculeManager.matches(REGEX_MATRICULE_MANAGER)) {
        	throw new BatchException ("la chaîne " + matriculeManager + " ne respecte pas l'expression régulière ^M[0-9]{5}$");
        }
        try {
			t.setGrade(grade);
		} catch (TechnicienException e) {
			throw new BatchException (e.getMessage());
		}
        checkCommonFields(ligneTechnicien, t);
        Manager m = managerRepository.findByMatricule(matriculeManager);
       	if (m != null) {
        	t.setManager(m);
       	} else {
       		for (int i = 0; i < employes.size(); i++) {
	        	if (matriculeManager.equals(employes.get(i).getMatricule())) {
	        		t.setManager((Manager) employes.get(i));
	        		break;
	        	}
       		}
       	}
       	if(t.getManager() == null) {
    		throw new BatchException ("Le manager de matricule " + matriculeManager + " n'a pas été trouvé dans le fichier ou en base de données");
    	}
       	employes.add(t);
    }
    
    private void checkCommonFields (String[] ligne, Employe emp) throws BatchException {
    	LocalDate dateEmbauche;
    	Double salaire;
	    try {
	    	dateEmbauche = DateTimeFormat.forPattern("dd/MM/yyyy").parseLocalDate(ligne[3]);
	    	
	    } catch (Exception e) {
	    	throw new BatchException (ligne[3] + " ne respecte pas le format de date dd/MM/yyyy");
	    }
	    try {
	    	salaire = Double.parseDouble(ligne[4]);
	    } catch (Exception e) {
	    	throw new BatchException (ligne[4] + " n'est pas un nombre valide pour un salaire");
	    }
	    try {
	    	emp.setDateEmbauche(dateEmbauche);
	    } catch (Exception e) {
	    	throw new BatchException (e.getMessage());
	    }
	    emp.setNom(ligne[1]);
	    emp.setPrenom(ligne[2]);
	    emp.setMatricule(ligne[0]);
	    emp.setSalaire(salaire);
    }
    
    
}
