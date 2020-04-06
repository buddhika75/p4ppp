package cwcdh.pppp.bean;

// <editor-fold defaultstate="collapsed" desc="Import">
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import cwcdh.pppp.entity.Solution;
import cwcdh.pppp.bean.util.JsfUtil;
import cwcdh.pppp.bean.util.JsfUtil.PersistAction;
import cwcdh.pppp.facade.SolutionFacade;
import java.io.Serializable;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.TimeZone;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.ejb.EJB;
import javax.ejb.EJBException;
import javax.inject.Named;
import javax.enterprise.context.SessionScoped;
import javax.faces.component.UIComponent;
import javax.faces.context.FacesContext;
import javax.faces.convert.Converter;
import javax.faces.convert.FacesConverter;
import javax.inject.Inject;
import javax.persistence.TemporalType;
import jxl.Cell;
import jxl.Sheet;
import jxl.Workbook;
import jxl.read.biff.BiffException;
import cwcdh.pppp.entity.Area;
import cwcdh.pppp.entity.Implementation;
import cwcdh.pppp.entity.Institution;
import cwcdh.pppp.entity.Item;
import cwcdh.pppp.entity.Person;
import cwcdh.pppp.entity.Relationship;
import cwcdh.pppp.entity.SiComponentItem;
import cwcdh.pppp.enums.AreaType;
import cwcdh.pppp.enums.EncounterType;
import cwcdh.pppp.enums.InstitutionType;
import cwcdh.pppp.enums.RelationshipType;
import cwcdh.pppp.facade.ImplementationFacade;
import cwcdh.pppp.pojcs.YearMonthDay;
import org.bouncycastle.jcajce.provider.digest.GOST3411;
import org.primefaces.component.tabview.TabView;
import org.primefaces.event.TabChangeEvent;
import org.primefaces.model.UploadedFile;
// </editor-fold>

@Named
@SessionScoped
public class SolutionController implements Serializable {

    // <editor-fold defaultstate="collapsed" desc="EJBs">
    @EJB
    private cwcdh.pppp.facade.SolutionFacade ejbFacade;
    @EJB
    private ImplementationFacade encounterFacade;
    // </editor-fold>
    // <editor-fold defaultstate="collapsed" desc="Controllers">
    @Inject
    ApplicationController applicationController;
    @Inject
    private WebUserController webUserController;
    @Inject
    private EncounterController encounterController;
    @Inject
    private ItemController itemController;
    @Inject
    private InstitutionController institutionController;
    @Inject
    private CommonController commonController;
    @Inject
    private AreaController areaController;
    @Inject
    SiComponentItemController siComponentItemController;
    // </editor-fold>
    // <editor-fold defaultstate="collapsed" desc="Variables">
    private List<Solution> items = null;
    private List<Solution> selectedSolutions = null;
    private List<Solution> importedClients = null;
    private Solution selected;
    private Long idFrom;
    private Long idTo;
    private Institution institution;
    private List<Implementation> selectedClientsClinics;
    private String searchingId;
    private Item item;
    private SiComponentItem siComponentItem;
    private List<SiComponentItem> selectedItems;
    @Deprecated
    private String searchingPhn;
    @Deprecated
    private String searchingPassportNo;
    @Deprecated
    private String searchingDrivingLicenceNo;
    @Deprecated
    private String searchingNicNo;
    private String searchingName;
    @Deprecated
    private String searchingPhoneNumber;
    @Deprecated
    private String uploadDetails;
    @Deprecated
    private String errorCode;
    private YearMonthDay yearMonthDay;
    private Institution selectedClinic;
    private int profileTabActiveIndex;
    private boolean goingToCaptureWebCamPhoto;
    private UploadedFile file;
    private Date clinicDate;
    private Date from;
    private Date to;

    private Boolean nicExists;
    private Boolean phnExists;
    private Boolean passportExists;
    private Boolean dlExists;

    // </editor-fold>
    // <editor-fold defaultstate="collapsed" desc="Constructors">
    public SolutionController() {
    }

    // </editor-fold>
    // <editor-fold defaultstate="collapsed" desc="Navigation">
    public String toSearchClientById() {
        return "/solution/search_by_name";
    }

    public String toSearchClientByDetails() {
        return "/solution/search_by_details";

    }

    public String toSelectSolution() {
        return "/solution/select";
    }
    
    public String toListAllSolutions() {
        String j = "select s from Solution s "
                + " where s.retired=:ret "
                + " order by s.name";
        Map m = new HashMap();
        m.put("ret", false);
        selectedSolutions = getFacade().findByJpql(j, m);
        return "/solution/select";
    }

    public String toEditSolution() {
        return "/solution/solution";
    }

    public String toSolutionProfile() {
        selectedClientsClinics = null;
        return "/solution/profile";
    }

    public String toAddNewClient() {
        selected = new Solution();
        selectedClientsClinics = null;
        selectedClinic = null;
        yearMonthDay = new YearMonthDay();
        return "/solution/solution";
    }

    // </editor-fold>
    // <editor-fold defaultstate="collapsed" desc="Functions">
    public List<Area> getGnAreasForTheSelectedClient(String qry) {
        List<Area> areas = new ArrayList<>();
        if (selected == null) {
            return areas;
        }
        if (selected.getPerson().getDsArea() == null) {
            return areaController.getAreas(AreaType.GN, null, null, qry);
        } else {
            return areaController.getAreas(AreaType.GN, selected.getPerson().getDsArea(), null, qry);
        }
    }

    @Deprecated
    public void clearExistsValues() {
        phnExists = false;
        nicExists = false;
        passportExists = false;
        dlExists = false;
    }

    public void itemChanged() {
        if (getItem() == null) {
            return;
        }
        if (getSiComponentItem() == null) {
            return;
        }
        getSiComponentItem().setItem(item);
        saveSolutionSilantly();
    }

    public void checkPhnExists() {
        phnExists = null;
        if (selected == null) {
            return;
        }
        if (selected.getPhn() == null) {
            return;
        }
        if (selected.getPhn().trim().equals("")) {
            return;
        }
        phnExists = checkPhnExists(selected.getPhn(), selected);
    }

    public Boolean checkPhnExists(String phn, Solution c) {
        String jpql = "select count(c) from Solution c "
                + " where c.retired=:ret "
                + " and c.phn=:phn ";
        Map m = new HashMap();
        m.put("ret", false);
        m.put("phn", phn);
        if (c != null && c.getId() != null) {
            jpql += " and c <> :solution";
            m.put("solution", c);
        }
        Long count = getFacade().countByJpql(jpql, m);
        if (count == null || count == 0l) {
            return false;
        } else {
            return true;
        }

    }

    public void checkNicExists() {
        nicExists = null;
        if (selected == null) {
            return;
        }
        if (selected.getPerson() == null) {
            return;
        }
        if (selected.getPerson().getNic() == null) {
            return;
        }
        if (selected.getPerson().getNic().trim().equals("")) {
            return;
        }
        nicExists = checkNicExists(selected.getPerson().getNic(), selected);
    }

    public Boolean checkNicExists(String nic, Solution c) {
        String jpql = "select count(c) from Solution c "
                + " where c.retired=:ret "
                + " and c.person.nic=:nic ";
        Map m = new HashMap();
        m.put("ret", false);
        m.put("nic", nic);
        if (c != null && c.getPerson() != null && c.getPerson().getId() != null) {
            jpql += " and c.person <> :person";
            m.put("person", c.getPerson());
        }
        Long count = getFacade().countByJpql(jpql, m);
        if (count == null || count == 0l) {
            return false;
        } else {
            return true;
        }

    }

    public void fixClientPersonCreatedAt() {
        String j = "select c from Solution c "
                + " where c.retired=:ret ";
        Map m = new HashMap();
        m.put("ret", false);
        List<Solution> cs = getFacade().findByJpql(j, m);
        for (Solution c : cs) {

            if (c.getCreatedAt() == null && c.getPerson().getCreatedAt() != null) {
                c.setCreatedAt(c.getPerson().getCreatedAt());
                getFacade().edit(c);
            } else if (c.getCreatedAt() != null && c.getPerson().getCreatedAt() == null) {
                c.getPerson().setCreatedAt(c.getCreatedAt());
                getFacade().edit(c);
            } else if (c.getCreatedAt() == null && c.getPerson().getCreatedAt() == null) {
                c.getPerson().setCreatedAt(new Date());
                c.setCreatedAt(new Date());
                getFacade().edit(c);
            }

        }

    }

    public void updateClientCreatedInstitution() {
        if (institution == null) {
            JsfUtil.addErrorMessage("Institution ?");
            return;
        }
        String j = "select c from Solution c "
                + " where c.retired=:ret "
                + " and c.id > :idf "
                + " and c.id < :idt ";
        Map m = new HashMap();
        m.put("ret", false);
        m.put("idf", idFrom);
        m.put("idt", idTo);
        List<Solution> cs = getFacade().findByJpql(j, m);
        for (Solution c : cs) {
            c.setCreateInstitution(institution);
            getFacade().edit(c);
        }

    }

    public void updateClientDateOfBirth() {
        String j = "select c from Solution c "
                + " where c.retired=:ret "
                + " and c.id > :idf "
                + " and c.id < :idt ";
        Map m = new HashMap();
        m.put("ret", false);
        m.put("idf", idFrom);
        m.put("idt", idTo);
        List<Solution> cs = getFacade().findByJpql(j, m);
        for (Solution c : cs) {
            Calendar cd = Calendar.getInstance();

            if (c.getPerson().getDateOfBirth() != null) {

                cd.setTime(c.getPerson().getDateOfBirth());

                int dobYear = cd.get(Calendar.YEAR);

                if (dobYear < 1800) {
                    cd.add(Calendar.YEAR, 2000);
                    c.getPerson().setDateOfBirth(cd.getTime());
                    getFacade().edit(c);
                }

            }
        }

    }

    public Long countOfRegistedClients(Institution ins, Area gn) {
        String j = "select count(c) from Solution c "
                + " where c.retired=:ret ";
        Map m = new HashMap();
        m.put("ret", false);
        if (ins != null) {
            j += " and c.createInstitution=:ins ";
            m.put("ins", ins);
        }
        if (gn != null) {
            j += " and c.person.gnArea=:gn ";
            m.put("gn", gn);
        }
        return getFacade().countByJpql(j, m);
    }

    public String toRegisterdClientsDemo() {
        String j = "select c from Solution c "
                + " where c.retired=:ret ";
        Map m = new HashMap();
        m.put("ret", false);
        if (webUserController.getLoggedUser().getInstitution() != null) {
            j += " and c.createInstitution=:ins ";
            m.put("ins", webUserController.getLoggedUser().getInstitution());
        } else {
            items = new ArrayList<>();
        }

        items = getFacade().findByJpql(j, m);
        return "/insAdmin/registered_clients";
    }

    public String toRegisterdClientsWithDates() {
        String j = "select c from Solution c "
                + " where c.retired=:ret ";
        Map m = new HashMap();
        m.put("ret", false);
        if (webUserController.getLoggedUser().getInstitution() != null) {
            j += " and c.createInstitution=:ins ";
            m.put("ins", webUserController.getLoggedUser().getInstitution());
        }
        j = j + " and c.createdAt between :fd and :td ";
        j = j + " order by c.id desc";
        m.put("fd", getFrom());
        m.put("td", getTo());
        items = getFacade().findByJpql(j, m, TemporalType.TIMESTAMP);
        return "/insAdmin/registered_clients";
    }

    public String toRegisterdClientsWithDatesForSystemAdmin() {
        String j = "select c from Solution c "
                + " where c.retired=:ret ";
        Map m = new HashMap();
        m.put("ret", false);
        j = j + " and c.createdAt between :fd and :td ";
        j = j + " order by c.id desc";
        m.put("fd", getFrom());
        m.put("td", getTo());
        selectedSolutions = null;
        items = getFacade().findByJpql(j, m, TemporalType.TIMESTAMP);
        return "/systemAdmin/all_clients";
    }

    public void saveSelectedImports() {
        if (institution == null) {
            JsfUtil.addErrorMessage("Institution ?");
            return;
        }
        for (Solution c : selectedSolutions) {
            c.setCreateInstitution(institution);
            if (!checkPhnExists(c.getPhn(), null)) {
                c.setId(null);
                saveSolution(c);
            }
        }
    }

    public void fillClientsWithWrongPhnLength() {
        String j = "select c from Solution c where length(c.phn) <>11 order by c.id";
        items = getFacade().findByJpql(j);
    }

    public String fillRetiredClients() {
        String j = "select c from Solution c "
                + " where c.retired=:ret ";
        Map m = new HashMap();
        m.put("ret", true);
        j = j + " and c.createdAt between :fd and :td ";
        j = j + " order by c.id desc";
        m.put("fd", getFrom());
        m.put("td", getTo());
        selectedSolutions = null;
        items = getFacade().findByJpql(j, m, TemporalType.TIMESTAMP);
        return "/systemAdmin/all_clients";
    }

    public String retireSelectedClients() {
        for (Solution c : selectedSolutions) {
            c.setRetired(true);
            c.setRetireComments("Bulk Delete");
            c.setRetiredAt(new Date());
            c.setRetiredBy(webUserController.getLoggedUser());

            c.getPerson().setRetired(true);
            c.getPerson().setRetireComments("Bulk Delete");
            c.getPerson().setRetiredAt(new Date());
            c.getPerson().setRetiredBy(webUserController.getLoggedUser());

            getFacade().edit(c);
        }
        selectedSolutions = null;
        return toRegisterdClientsWithDatesForSystemAdmin();
    }

    public String unretireSelectedClients() {
        for (Solution c : selectedSolutions) {
            c.setRetired(false);
            c.setRetireComments("Bulk Un Delete");
            c.setLastEditBy(webUserController.getLoggedUser());
            c.setLastEditeAt(new Date());

            c.getPerson().setRetired(false);
            c.getPerson().setRetireComments("Bulk Un Delete");
            c.getPerson().setEditedAt(new Date());
            c.getPerson().setEditer(webUserController.getLoggedUser());

            getFacade().edit(c);
        }
        selectedSolutions = null;
        return toRegisterdClientsWithDatesForSystemAdmin();
    }

    public void retireSelectedClient() {
        Solution c = selected;
        if (c != null) {
            c.setRetired(true);
            c.setRetiredBy(webUserController.getLoggedUser());
            c.setRetiredAt(new Date());

            c.getPerson().setRetired(true);
            c.getPerson().setRetiredBy(webUserController.getLoggedUser());
            c.getPerson().setRetiredAt(new Date());

            getFacade().edit(c);
        }
    }

    public void saveAllImports() {
        if (institution == null) {
            JsfUtil.addErrorMessage("Institution ?");
            return;
        }
        for (Solution c : importedClients) {
            c.setCreateInstitution(institution);
            if (!checkPhnExists(c.getPhn(), null)) {
                c.setId(null);
                saveSolution(c);
            }
        }
    }

//    public boolean phnExists(String phn) {
//        String j = "select c from Solution c where c.retired=:ret "
//                + " and c.phn=:phn";
//        Map m = new HashMap();
//        m.put("ret", false);
//        m.put("phn", phn);
//        Solution c = getFacade().findFirstByJpql(j, m);
//        if (c == null) {
//            return false;
//        }
//        return true;
//    }
    public String importClientsFromExcel() {

        importedClients = new ArrayList<>();

        if (uploadDetails == null || uploadDetails.trim().equals("")) {
            JsfUtil.addErrorMessage("Add Column Names");
            return "";
        }

        String[] cols = uploadDetails.split("\\r?\\n");
        if (cols == null || cols.length < 5) {
            JsfUtil.addErrorMessage("No SUfficient Columns");
            return "";
        }

        try {
            File inputWorkbook;
            Workbook w;
            Cell cell;
            InputStream in;
            try {
                in = file.getInputstream();
                File f;
                f = new File(Calendar.getInstance().getTimeInMillis() + file.getFileName());
                FileOutputStream out = new FileOutputStream(f);
                int read = 0;
                byte[] bytes = new byte[1024];
                while ((read = in.read(bytes)) != -1) {
                    out.write(bytes, 0, read);
                }
                in.close();
                out.flush();
                out.close();

                inputWorkbook = new File(f.getAbsolutePath());

                JsfUtil.addSuccessMessage("Excel File Opened");
                w = Workbook.getWorkbook(inputWorkbook);
                Sheet sheet = w.getSheet(0);

                errorCode = "";

                int startRow = 1;

                Long temId = 0L;

                for (int i = startRow; i < sheet.getRows(); i++) {

                    Map m = new HashMap();

                    Solution c = new Solution();
                    Person p = new Person();
                    c.setPerson(p);

                    int colNo = 0;

                    for (String colName : cols) {
                        cell = sheet.getCell(colNo, i);
                        String cellString = cell.getContents();
                        switch (colName) {
                            case "client_name":
                                c.getPerson().setName(cellString);
                                break;
                            case "client_phn_number":
                                c.setPhn(cellString);
                                break;
                            case "client_sex":
                                Item sex;
                                if (cellString.toLowerCase().contains("f")) {
                                    sex = itemController.findItemByCode("sex_female");
                                } else {
                                    sex = itemController.findItemByCode("sex_male");
                                }
                                c.getPerson().setSex(sex);
                                break;
                            case "client_citizenship":
                                Item cs;
                                if (cellString == null) {
                                    cs = null;
                                } else if (cellString.toLowerCase().contains("sri")) {
                                    cs = itemController.findItemByCode("citizenship_local");
                                } else {
                                    cs = itemController.findItemByCode("citizenship_foreign");
                                }
                                c.getPerson().setCitizenship(cs);
                                break;

                            case "client_ethnic_group":
                                Item eg = null;
                                if (cellString == null || cellString.trim().equals("")) {
                                    eg = null;
                                } else if (cellString.equalsIgnoreCase("Sinhala")) {
                                    eg = itemController.findItemByCode("sinhalese");
                                } else if (cellString.equalsIgnoreCase("moors")) {
                                    eg = itemController.findItemByCode("citizenship_local");
                                } else if (cellString.equalsIgnoreCase("SriLankanTamil")) {
                                    eg = itemController.findItemByCode("tamil");
                                } else {
                                    eg = itemController.findItemByCode("ethnic_group_other");;
                                }
                                c.getPerson().setEthinicGroup(eg);
                                break;
                            case "client_religion":
                                Item re = null;
                                if (cellString == null || cellString.trim().equals("")) {
                                    re = null;
                                } else if (cellString.equalsIgnoreCase("Buddhist")) {
                                    re = itemController.findItemByCode("buddhist");
                                } else if (cellString.equalsIgnoreCase("Christian")) {
                                    re = itemController.findItemByCode("christian");
                                } else if (cellString.equalsIgnoreCase("Hindu")) {
                                    re = itemController.findItemByCode("hindu");
                                } else {
                                    re = itemController.findItemByCode("religion_other");;
                                }
                                c.getPerson().setReligion(re);
                                break;
                            case "client_marital_status":
                                Item ms = null;
                                if (cellString == null || cellString.trim().equals("")) {
                                    ms = null;
                                } else if (cellString.equalsIgnoreCase("Married")) {
                                    ms = itemController.findItemByCode("married");
                                } else if (cellString.equalsIgnoreCase("Separated")) {
                                    ms = itemController.findItemByCode("seperated");
                                } else if (cellString.equalsIgnoreCase("Single")) {
                                    ms = itemController.findItemByCode("unmarried");
                                } else {
                                    ms = itemController.findItemByCode("marital_status_other");;
                                }
                                c.getPerson().setMariatalStatus(ms);
                                break;
                            case "client_title":
                                Item title = null;
                                String ts = cellString;
                                switch (ts) {
                                    case "Baby":
                                        title = itemController.findItemByCode("baby");
                                        break;
                                    case "Babyof":
                                        title = itemController.findItemByCode("baby_of");
                                        break;
                                    case "Mr":
                                        title = itemController.findItemByCode("mr");
                                        break;
                                    case "Mrs":
                                        title = itemController.findItemByCode("mrs");
                                        break;
                                    case "Ms":
                                        title = itemController.findItemByCode("ms");
                                        break;
                                    case "Prof":
                                        title = itemController.findItemByCode("prof");
                                        break;
                                    case "Rev":
                                    case "Thero":
                                        title = itemController.findItemByCode("rev");
                                        break;
                                }
                                c.getPerson().setTitle(title);
                                break;
                            case "client_nic_number":
                                c.getPerson().setNic(cellString);
                                break;
                            case "client_data_of_birth":
                                Date tdob = commonController.dateFromString(cellString, "yyyy/MM/dd");
                                c.getPerson().setDateOfBirth(tdob);
                                break;
                            case "client_permanent_address":
                                c.getPerson().setAddress(cellString);
                                break;
                            case "client_current_address":
                                c.getPerson().setAddress(cellString);
                                break;
                            case "client_mobile_number":
                                c.getPerson().setPhone1(cellString);
                                break;
                            case "client_home_number":
                                c.getPerson().setPhone2(cellString);
                                break;
                            case "client_registered_at":
                                Date reg = commonController.dateFromString(cellString, "MM/dd/yyyy hh:mm:ss");
                                c.getPerson().setCreatedAt(reg);
                                c.setCreatedAt(reg);
                                break;
                            case "client_gn_area":
                                System.out.println("GN");
                                System.out.println("cellString = " + cellString);

                                Area tgn = areaController.getAreaByName(cellString, AreaType.GN, false, null);
                                System.out.println("tgn = " + tgn);
                                if (tgn != null) {
                                    c.getPerson().setGnArea(tgn);
                                    c.getPerson().setDsArea(tgn.getDsd());
                                    c.getPerson().setMohArea(tgn.getMoh());
                                    c.getPerson().setPhmArea(tgn.getPhm());
                                    c.getPerson().setDistrict(tgn.getDistrict());
                                    c.getPerson().setProvince(tgn.getProvince());
                                }
                                break;
                        }

                        colNo++;
                    }

                    c.setId(temId);
                    temId++;

                    importedClients.add(c);

                }

                cwcdh.pppp.facade.util.JsfUtil.addSuccessMessage("Succesful. All the data in Excel File Impoted to the database");
                errorCode = "";
                return "save_imported_clients";
            } catch (IOException ex) {
                errorCode = ex.getMessage();
                cwcdh.pppp.facade.util.JsfUtil.addErrorMessage(ex.getMessage());
                return "";
            } catch (BiffException ex) {
                cwcdh.pppp.facade.util.JsfUtil.addErrorMessage(ex.getMessage());
                errorCode = ex.getMessage();
                return "";
            }
        } catch (IndexOutOfBoundsException e) {
            errorCode = e.getMessage();
            return "";
        }
    }

    public void prepareToCapturePhotoWithWebCam() {
        goingToCaptureWebCamPhoto = true;
    }

    public void finishCapturingPhotoWithWebCam() {
        goingToCaptureWebCamPhoto = false;
    }

    public void onTabChange(TabChangeEvent event) {

        // //System.out.println("profileTabActiveIndex = " + profileTabActiveIndex);
        TabView tabView = (TabView) event.getComponent();

        profileTabActiveIndex = tabView.getChildren().indexOf(event.getTab());

    }

    public List<Implementation> fillEncounters(Solution solution, InstitutionType insType, EncounterType encType, boolean excludeCompleted) {
        // //System.out.println("fillEncounters");
        String j = "select e from Implementation e where e.retired=false ";
        Map m = new HashMap();
        if (solution != null) {
            j += " and e.solution=:c ";
            m.put("c", solution);
        }
        if (insType != null) {
            j += " and e.institution.institutionType=:it ";
            m.put("it", insType);
        }
        if (insType != null) {
            j += " and e.encounterType=:et ";
            m.put("et", encType);
        }
        if (excludeCompleted) {
            j += " and e.completed=:com ";
            m.put("com", false);
        }
        // //System.out.println("m = " + m);
        return encounterFacade.findByJpql(j, m);
    }

    public void enrollInClinic() {
        if (selectedClinic == null) {
            JsfUtil.addErrorMessage("Please select an clinic to enroll.");
            return;
        }
        if (selected == null) {
            JsfUtil.addErrorMessage("Please select a solution to enroll.");
            return;
        }
        if (encounterController.clinicEnrolmentExists(selectedClinic, selected)) {
            JsfUtil.addErrorMessage("This solution is already enrolled.");
            return;
        }
        Implementation implementation = new Implementation();
        implementation.setClient(selected);
        implementation.setEncounterType(EncounterType.Clinic_Enroll);
        implementation.setCreatedAt(new Date());
        implementation.setCreatedBy(webUserController.getLoggedUser());
        implementation.setInstitution(selectedClinic);
        if (clinicDate != null) {
            implementation.setEncounterDate(clinicDate);
        } else {
            implementation.setEncounterDate(new Date());
        }
        implementation.setEncounterNumber(encounterController.createClinicEnrollNumber(selectedClinic));
        implementation.setCompleted(false);
        encounterFacade.create(implementation);
        JsfUtil.addSuccessMessage(selected.getPerson().getNameWithTitle() + " was Successfully Enrolled in " + selectedClinic.getName() + "\nThe Clinic number is " + implementation.getEncounterNumber());
        selectedClientsClinics = null;
    }

    public void generateAndAssignNewPhn() {
        if (selected == null) {
            return;
        }
        Institution poiIns;
        if (webUserController.getLoggedUser().getInstitution() == null) {
            JsfUtil.addErrorMessage("You do not have an Institution. Please contact support.");
            return;
        }
        System.out.println("webUserController.getLoggedUser().getInstitution() = " + webUserController.getLoggedUser().getInstitution().getLastHin());
        if (webUserController.getLoggedUser().getInstitution().getPoiInstitution() != null) {
            poiIns = webUserController.getLoggedUser().getInstitution().getPoiInstitution();
        } else {
            poiIns = webUserController.getLoggedUser().getInstitution();
        }
        if (poiIns.getPoiNumber() == null || poiIns.getPoiNumber().trim().equals("")) {
            JsfUtil.addErrorMessage("A Point of Issue is NOT assigned to your Institution. Please discuss with the System Administrator.");
            return;
        }
        selected.setPhn(applicationController.createNewPersonalHealthNumber(poiIns));

        if (webUserController.getLoggedUser().getInstitution().getPoiInstitution() != null) {
            webUserController.getLoggedUser().getInstitution().setPoiInstitution(institutionController.getInstitutionById(webUserController.getLoggedUser().getInstitution().getPoiInstitution().getId()));
            System.out.println("Last HIN case 1 = " + webUserController.getLoggedUser().getInstitution().getPoiInstitution().getLastHin());
        } else {
            webUserController.getLoggedUser().setInstitution(institutionController.getInstitutionById(webUserController.getLoggedUser().getInstitution().getId()));
            System.out.println("Last HIN Case 2 = " + webUserController.getLoggedUser().getInstitution().getLastHin());
        }

    }

    public void gnAreaChanged() {
        if (selected == null) {
            return;
        }
        if (selected.getPerson().getGnArea() != null) {
            selected.getPerson().setDsArea(selected.getPerson().getGnArea().getDsd());
            selected.getPerson().setMohArea(selected.getPerson().getGnArea().getMoh());
            selected.getPerson().setPhmArea(selected.getPerson().getGnArea().getPhm());
            selected.getPerson().setDistrict(selected.getPerson().getGnArea().getDistrict());
            selected.getPerson().setProvince(selected.getPerson().getGnArea().getProvince());
        }
    }

    public void updateYearDateMonth() {
        getYearMonthDay();
        if (selected != null) {
            yearMonthDay.setYear(selected.getPerson().getAgeYears() + "");
            yearMonthDay.setMonth(selected.getPerson().getAgeMonths() + "");
            yearMonthDay.setDay(selected.getPerson().getAgeDays() + "");
            selected.getPerson().setDobIsAnApproximation(false);
        } else {
            yearMonthDay = new YearMonthDay();
        }
    }

    public void yearMonthDateChanged() {
        if (selected == null) {
            return;
        }
        selected.getPerson().setDobIsAnApproximation(true);
        selected.getPerson().setDateOfBirth(guessDob(yearMonthDay));
    }

    public Date guessDob(YearMonthDay yearMonthDay) {
        // ////// //System.out.println("year string is " + docStr);
        int years = 0;
        int month = 0;
        int day = 0;
        Calendar now = Calendar.getInstance(TimeZone.getTimeZone("IST"));
        try {
            if (yearMonthDay.getYear() != null && !yearMonthDay.getYear().isEmpty()) {
                years = Integer.valueOf(yearMonthDay.getYear());
                now.add(Calendar.YEAR, -years);
            }

            if (yearMonthDay.getMonth() != null && !yearMonthDay.getMonth().isEmpty()) {
                month = Integer.valueOf(yearMonthDay.getMonth());
                now.add(Calendar.MONTH, -month);
            }

            if (yearMonthDay.getDay() != null && !yearMonthDay.getDay().isEmpty()) {
                day = Integer.valueOf(yearMonthDay.getDay());
                now.add(Calendar.DATE, -day);
            }

            return now.getTime();
        } catch (Exception e) {
            ////// //System.out.println("Error is " + e.getMessage());
            return new Date();

        }
    }

    public void addNewProperty() {
        if (selected == null) {
            JsfUtil.addErrorMessage("No Solution is Selected");
            return;
        }
        if (siComponentItem == null) {
            JsfUtil.addErrorMessage("No Property Item is Selected");
            return;
        }
        if (item == null) {
            JsfUtil.addErrorMessage("No Display Item is Selected");
            return;
        }
        siComponentItem.setItem(item);
        siComponentItem.setSolution(selected);
        siComponentItemController.save(siComponentItem);
        siComponentItem = new SiComponentItem();
        item = null;
        getSelectedItems();
    }

    public String searchByName() {
      selectedSolutions=  listSolutionsByName(searchingName);
        if (selectedSolutions == null || selectedSolutions.isEmpty()) {
            JsfUtil.addErrorMessage("No Results Found. Try different search criteria.");
            return "";
        }
        if (selectedSolutions.size() == 1) {
            selected = selectedSolutions.get(0);
            selectedSolutions = null;
            clearSearchByName();
            return toSolutionProfile();
        } else {
            selected = null;
            clearSearchByName();
            return toSelectSolution();
        }
    }

    public String searchByAnyId() {
        clearExistsValues();
        if (searchingId == null) {
            searchingId = "";
        }

        selectedSolutions = listPatientsByIDs(searchingId.trim().toUpperCase());

        if (selectedSolutions == null || selectedSolutions.isEmpty()) {
            JsfUtil.addErrorMessage("No Results Found. Try different search criteria.");
            return "/solution/search_by_name";
        }
        if (selectedSolutions.size() == 1) {
            selected = selectedSolutions.get(0);
            selectedSolutions = null;
            searchingId = "";
            return toSolutionProfile();
        } else {
            selected = null;
            searchingId = "";
            return toSelectSolution();
        }
    }

    public void clearSearchByName() {
        searchingId = "";
        searchingName = "";
    }

    @Deprecated
    public List<Solution> listPatientsByPhn(String phn) {
        String j = "select c from Solution c where c.retired=false and upper(c.phn)=:q order by c.phn";
        Map m = new HashMap();
        m.put("q", phn.trim().toUpperCase());
        return getFacade().findByJpql(j, m);
    }

    public List<Solution> listSolutionsByName(String phn) {
        String j = "select c from Solution c "
                + " where c.retired=false "
                + " and upper(c.name) like :q "
                + " order by c.name";
        Map m = new HashMap();
        m.put("q", "%" + phn.trim().toUpperCase() + "%");
        return getFacade().findByJpql(j, m);
    }

    @Deprecated
    public List<Solution> listPatientsByPhone(String phn) {
        String j = "select c from Solution c where c.retired=false and (upper(c.person.phone1)=:q or upper(c.person.phone2)=:q) order by c.phn";
        Map m = new HashMap();
        m.put("q", phn.trim().toUpperCase());
        return getFacade().findByJpql(j, m);
    }

    @Deprecated
    public List<Solution> listPatientsByIDs(String ids) {
        if (ids == null || ids.trim().equals("")) {
            return null;
        }
        String j = "select c from Solution c "
                + " where c.retired=false "
                + " and ("
                + " upper(c.person.phone1)=:q "
                + " or "
                + " upper(c.person.phone2)=:q "
                + " or "
                + " upper(c.person.nic)=:q "
                + " or "
                + " upper(c.phn)=:q "
                + " ) "
                + " order by c.phn";
        Map m = new HashMap();
        m.put("q", ids.trim().toUpperCase());
        return getFacade().findByJpql(j, m);
    }

    public Solution prepareCreate() {
        selected = new Solution();
        return selected;
    }

    public String saveSolution() {

        saveSolution(selected);
        JsfUtil.addSuccessMessage("Saved.");
        return toSolutionProfile();
    }
    
    public void saveSolutionSilantly() {
        saveSolution(selected);
    }

    public String saveSolution(Solution c) {
        if (c == null) {
            JsfUtil.addErrorMessage("No Solution Selected to save.");
            return "";
        }
        if (c.getId() == null) {
            c.setCreatedBy(webUserController.getLoggedUser());
            c.setCreatedAt(new Date());
            getFacade().create(c);
        } else {
            c.setLastEditBy(webUserController.getLoggedUser());
            c.setLastEditeAt(new Date());
            getFacade().edit(c);
        }
        return toSolutionProfile();
    }

    public void create() {
        persist(PersistAction.CREATE, ResourceBundle.getBundle("/BundleClinical").getString("ClientCreated"));
        if (!JsfUtil.isValidationFailed()) {
            items = null;    // Invalidate list of items to trigger re-query.
        }
    }

    public void update() {
        persist(PersistAction.UPDATE, ResourceBundle.getBundle("/BundleClinical").getString("ClientUpdated"));
    }

    public void destroy() {
        persist(PersistAction.DELETE, ResourceBundle.getBundle("/BundleClinical").getString("ClientDeleted"));
        if (!JsfUtil.isValidationFailed()) {
            selected = null; // Remove selection
            items = null;    // Invalidate list of items to trigger re-query.
        }
    }

    private void persist(PersistAction persistAction, String successMessage) {
        if (selected != null) {
            try {
                if (persistAction != PersistAction.DELETE) {
                    getFacade().edit(selected);
                } else {
                    getFacade().remove(selected);
                }
                JsfUtil.addSuccessMessage(successMessage);
            } catch (EJBException ex) {
                String msg = "";
                Throwable cause = ex.getCause();
                if (cause != null) {
                    msg = cause.getLocalizedMessage();
                }
                if (msg.length() > 0) {
                    JsfUtil.addErrorMessage(msg);
                } else {
                    JsfUtil.addErrorMessage(ex, ResourceBundle.getBundle("/BundleClinical").getString("PersistenceErrorOccured"));
                }
            } catch (Exception ex) {
                Logger.getLogger(this.getClass().getName()).log(Level.SEVERE, null, ex);
                JsfUtil.addErrorMessage(ex, ResourceBundle.getBundle("/BundleClinical").getString("PersistenceErrorOccured"));
            }
        }
    }

    // </editor-fold>
    // <editor-fold defaultstate="collapsed" desc="Getters & Setters">
    public String getSearchingId() {
        return searchingId;
    }

    public void setSearchingId(String searchingId) {
        this.searchingId = searchingId;
    }

    public String getSearchingPhn() {
        return searchingPhn;
    }

    public void setSearchingPhn(String searchingPhn) {
        this.searchingPhn = searchingPhn;
    }

    public String getSearchingPassportNo() {
        return searchingPassportNo;
    }

    public void setSearchingPassportNo(String searchingPassportNo) {
        this.searchingPassportNo = searchingPassportNo;
    }

    public String getSearchingDrivingLicenceNo() {
        return searchingDrivingLicenceNo;
    }

    public void setSearchingDrivingLicenceNo(String searchingDrivingLicenceNo) {
        this.searchingDrivingLicenceNo = searchingDrivingLicenceNo;
    }

    public String getSearchingNicNo() {
        return searchingNicNo;
    }

    public void setSearchingNicNo(String searchingNicNo) {
        this.searchingNicNo = searchingNicNo;
    }

    public String getSearchingName() {
        return searchingName;
    }

    public void setSearchingName(String searchingName) {
        this.searchingName = searchingName;
    }

    public SolutionFacade getEjbFacade() {
        return ejbFacade;
    }

    public ApplicationController getApplicationController() {
        return applicationController;
    }

    public Solution getSelected() {
        return selected;
    }

    public void setSelected(Solution selected) {
        this.selected = selected;
        selectedItems = null;
    }

    private SolutionFacade getFacade() {
        return ejbFacade;
    }

    public List<Solution> getItems() {
//        if (items == null) {
//            items = getFacade().findAll();
//        }
        return items;
    }

    public List<Solution> getItems(String jpql, Map m) {
        return getFacade().findByJpql(jpql, m);
    }

    public Solution getClient(java.lang.Long id) {
        return getFacade().find(id);
    }

    public List<Solution> getItemsAvailableSelectMany() {
        return getFacade().findAll();
    }

    public List<Solution> getItemsAvailableSelectOne() {
        return getFacade().findAll();
    }

    public WebUserController getWebUserController() {
        return webUserController;
    }

    public String getSearchingPhoneNumber() {
        return searchingPhoneNumber;
    }

    public void setSearchingPhoneNumber(String searchingPhoneNumber) {
        this.searchingPhoneNumber = searchingPhoneNumber;
    }

    public List<Solution> getSelectedSolutions() {
        return selectedSolutions;
    }

    public void setSelectedSolutions(List<Solution> selectedSolutions) {
        this.selectedSolutions = selectedSolutions;
    }

    public YearMonthDay getYearMonthDay() {
        if (yearMonthDay == null) {
            yearMonthDay = new YearMonthDay();
        }
        return yearMonthDay;
    }

    public void setYearMonthDay(YearMonthDay yearMonthDay) {
        this.yearMonthDay = yearMonthDay;
    }

    public Institution getSelectedClinic() {
        return selectedClinic;
    }

    public void setSelectedClinic(Institution selectedClinic) {
        this.selectedClinic = selectedClinic;
    }

    public List<Implementation> getSelectedClientsClinics() {
        if (selectedClientsClinics == null) {
            selectedClientsClinics = fillEncounters(selected, InstitutionType.Clinic, EncounterType.Clinic_Enroll, true);
        }
        return selectedClientsClinics;
    }

    public void setSelectedClientsClinics(List<Implementation> selectedClientsClinics) {
        this.selectedClientsClinics = selectedClientsClinics;
    }

    public int getProfileTabActiveIndex() {
        return profileTabActiveIndex;
    }

    public void setProfileTabActiveIndex(int profileTabActiveIndex) {
        this.profileTabActiveIndex = profileTabActiveIndex;
    }

    public ImplementationFacade getEncounterFacade() {
        return encounterFacade;
    }

    public EncounterController getEncounterController() {
        return encounterController;
    }

    public boolean isGoingToCaptureWebCamPhoto() {
        return goingToCaptureWebCamPhoto;
    }

    public void setGoingToCaptureWebCamPhoto(boolean goingToCaptureWebCamPhoto) {
        this.goingToCaptureWebCamPhoto = goingToCaptureWebCamPhoto;
    }

    public String getUploadDetails() {
        if (uploadDetails == null || uploadDetails.trim().equals("")) {
            uploadDetails
                    = "client_phn_number" + "\n"
                    + "client_nic_number" + "\n"
                    + "client_title" + "\n"
                    + "client_name" + "\n"
                    + "client_sex" + "\n"
                    + "client_data_of_birth" + "\n"
                    + "client_citizenship" + "\n"
                    + "client_ethnic_group" + "\n"
                    + "client_religion" + "\n"
                    + "client_marital_status" + "\n"
                    + "client_permanent_address" + "\n"
                    + "client_gn_area" + "\n"
                    + "client_mobile_number" + "\n"
                    + "client_home_number" + "\n"
                    + "client_email" + "\n"
                    + "client_registered_at" + "\n";
        }

        return uploadDetails;
    }

    public void setUploadDetails(String uploadDetails) {
        this.uploadDetails = uploadDetails;
    }

    public UploadedFile getFile() {
        return file;
    }

    public void setFile(UploadedFile file) {
        this.file = file;
    }

    public List<Solution> getImportedClients() {
        return importedClients;
    }

    public void setImportedClients(List<Solution> importedClients) {
        this.importedClients = importedClients;
    }

    public String getErrorCode() {
        return errorCode;
    }

    public void setErrorCode(String errorCode) {
        this.errorCode = errorCode;
    }

    public ItemController getItemController() {
        return itemController;
    }

    public CommonController getCommonController() {
        return commonController;
    }

    public AreaController getAreaController() {
        return areaController;
    }

    public Institution getInstitution() {
        return institution;
    }

    public void setInstitution(Institution institution) {
        this.institution = institution;
    }

    public Long getIdFrom() {
        return idFrom;
    }

    public void setIdFrom(Long idFrom) {
        this.idFrom = idFrom;
    }

    public Long getIdTo() {
        return idTo;
    }

    public void setIdTo(Long idTo) {
        this.idTo = idTo;
    }

    public Date getClinicDate() {
        return clinicDate;
    }

    public void setClinicDate(Date clinicDate) {
        this.clinicDate = clinicDate;
    }

    public Boolean getNicExists() {
        return nicExists;
    }

    public void setNicExists(Boolean nicExists) {
        this.nicExists = nicExists;
    }

    public Boolean getPhnExists() {
        return phnExists;
    }

    public void setPhnExists(Boolean phnExists) {
        this.phnExists = phnExists;
    }

    public Boolean getPassportExists() {
        return passportExists;
    }

    public void setPassportExists(Boolean passportExists) {
        this.passportExists = passportExists;
    }

    public Boolean getDlExists() {
        return dlExists;
    }

    public void setDlExists(Boolean dlExists) {
        this.dlExists = dlExists;
    }

    public InstitutionController getInstitutionController() {
        return institutionController;
    }

    public void setInstitutionController(InstitutionController institutionController) {
        this.institutionController = institutionController;
    }

    public Date getFrom() {
        if (from == null) {
            from = commonController.startOfTheDay();
        }
        return from;
    }

    public void setFrom(Date from) {
        this.from = from;
    }

    public Date getTo() {
        if (to == null) {
            to = commonController.endOfTheDay();
        }
        return to;
    }

    public void setTo(Date to) {
        this.to = to;
    }

    public Item getItem() {
        return item;
    }

    public void setItem(Item item) {
        this.item = item;
    }

    public SiComponentItem getSiComponentItem() {
        if (siComponentItem == null) {
            siComponentItem = new SiComponentItem();
        }
        siComponentItem.setItem(item);
        return siComponentItem;
    }

    public void setSiComponentItem(SiComponentItem siComponentItem) {
        this.siComponentItem = siComponentItem;
    }

    public List<SiComponentItem> getSelectedItems() {
        System.out.println("getSelectedItems");
        if (selected == null) {
            System.out.println("selected is Null. Returning.");
            return new ArrayList<>();
        }
        selectedItems = siComponentItemController.findSolutionItems(selected);
        System.out.println("Items from Database " + selectedItems);
        if (selectedItems == null) {
            System.out.println("selectedItems is null. Getting from Database. Selected is " + selected.getName());
            selectedItems = siComponentItemController.findSolutionItems(selected);
            System.out.println("Items from Database " + selectedItems);
        }
        if (selectedItems == null) {
            System.out.println("selectedItems is still null. Creating an empty list.");
            selectedItems = new ArrayList<>();
        }
        
        return selectedItems;
    }

    public void setSelectedItems(List<SiComponentItem> selectedItems) {
        this.selectedItems = selectedItems;
    }

    // </editor-fold>
    // <editor-fold defaultstate="collapsed" desc="Inner Classes">
    // </editor-fold>
    // <editor-fold defaultstate="collapsed" desc="Converters">
    @FacesConverter(forClass = Solution.class)
    public static class solutionControllerConverter implements Converter {

        @Override
        public Object getAsObject(FacesContext facesContext, UIComponent component, String value) {
            if (value == null || value.length() == 0) {
                return null;
            }
            SolutionController controller = (SolutionController) facesContext.getApplication().getELResolver().
                    getValue(facesContext.getELContext(), null, "solutionController");
            return controller.getClient(getKey(value));
        }

        java.lang.Long getKey(String value) {
            java.lang.Long key;
            key = Long.valueOf(value);
            return key;
        }

        String getStringKey(java.lang.Long value) {
            StringBuilder sb = new StringBuilder();
            sb.append(value);
            return sb.toString();
        }

        @Override
        public String getAsString(FacesContext facesContext, UIComponent component, Object object) {
            if (object == null) {
                return null;
            }
            if (object instanceof Solution) {
                Solution o = (Solution) object;
                return getStringKey(o.getId());
            } else {
                Logger.getLogger(this.getClass().getName()).log(Level.SEVERE, "object {0} is of type {1}; expected type: {2}", new Object[]{object, object.getClass().getName(), Solution.class.getName()});
                return null;
            }
        }

    }

    // </editor-fold>
}