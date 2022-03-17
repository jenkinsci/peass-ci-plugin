package de.dagere.peass.ci.clean;

class CleaningResult {
   
   public static final String SUCCESS_COLOR = "#00FF00";
   public static final String FAILURE_COLOR = "#FF0000";
   
   private String color;
   private String message;

   public CleaningResult() {
   }

   public CleaningResult(String color, String message) {
      this.color = color;
      this.message = message;
   }

   public String getColor() {
      return color;
   }

   public void setColor(String color) {
      this.color = color;
   }

   public String getMessage() {
      return message;
   }

   public void setMessage(String message) {
      this.message = message;
   }
}