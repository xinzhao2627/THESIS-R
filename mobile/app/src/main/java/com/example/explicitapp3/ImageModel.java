package com.example.explicitapp3;
/**
IMAGE MODEL DOCUMENTATION
<p> This is the image function for the overlay foreground
Example
{@code
    ImageModel imageModel = new ImageModel();
    imageModel.initModel();
    imageModel.destroy();
}

@author xinzhao2627 (R. Montaniel)
@see MainActivity#clasa
*/
public class ImageModel {
   Interpreter interpreter;

   /** TODO: 
   * initilizeModel
   * @param: String path
   * @param: Context mcontext
     @impliednote: tflite types are isolated, check assets if include metadata
   */ 
   public void initModel(){}

   public void pauseState(boolean _state)     {
      /** 
        tflite states are mutable
        TODO:
          - 
      */
}
   public void destroy(){}

}
