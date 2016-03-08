package exceptions;

public class InvalidInstructionsException extends Exception
{
    private String errorString;

    public InvalidInstructionsException(String errors)
    {
        this.errorString = errors;
    }

    public String getErrorStringForUsers()
    {
        return "The given instruction string is invalid.\n"+this.errorString;
    }

    public void printStackTrace()
    {
        super.printStackTrace();
        System.out.println(this.errorString);
    }
}
