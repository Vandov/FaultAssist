package hu.bme.iit.faultassist;


/** Elements of the issue table. Used to store the name, id and if the node is visited or not.
 *  Stored in a list in IssueList.
 *  TODO: switch from IssueList **/
public class IssueElement {
    String id;
    String cause;
    Boolean visited = false;
}
