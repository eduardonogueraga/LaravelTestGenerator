package com.rev;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.*;

public class Main {

    static final String Directory = "C:/Users/isrev/IdeaProjects/LaravelTestGenerator/src/com/rev/"; //Ruta Dev
    //static final String Directory = System.getProperty("user.dir")+ "\\"; //Ruta Build
    static boolean isUpdatingTest = false;
    static boolean withWebRoute = false;
    static String modelName;
    static String modelNameSingular;
    static JSONArray databaseNames;
    static String controllerName;
    static String routeUriName;
    static String salida = "";

    public static void main(String[] args) {

        Scanner uiInput = new Scanner(System.in);
        JSONParser parser  = new JSONParser();
        HashMap<String, ArrayList<String>> requestFields = new HashMap<>(); //Datos JSON
        LinkedHashMap<String, String> equivalents = new LinkedHashMap<>(); //Equivalencias (Campos -> Tabla BD)

        loadRequestRules(parser, requestFields, equivalents);
        selectOption(uiInput);
        if (withWebRoute)
        generateWebRoutes();
        generateFeatureTest(requestFields);
        loadBaseTest(equivalents);

        System.out.println(salida);
    }

    private static void selectOption(Scanner uiInput) {

        System.out.println("Laravel Test Generator v1.0.0 | https://github.com/eduardonogueraga\n");
        System.out.println("AVISO Es necesario revisar manualmente los test (UpdateTest Base, Nullable Test))");
        System.out.println("Elija el tipo de test a generar (POST o PUT)");
        System.out.println("Create Test -> C | Update Test -> U");

        switch (uiInput.nextLine().toUpperCase()){
            case "C":
                break;
            case "U":
                isUpdatingTest = true;
                break;
            default:
                System.out.println("Acción no valida");
                System.exit(0);
        }
        System.out.println("Crear ademas rutas /Web.php (Omitir si ya estan creadas)");
        System.out.println("Crear -> C | Omitir -> Any");

        switch (uiInput.nextLine().toUpperCase()){
            case "C":
                withWebRoute = true;
                break;
            default:
                System.out.println("Omitido: creacion de rutas");
        }

    }

    private static void generateFeatureTest(HashMap<String, ArrayList<String>> requestFields) {
        for (Map.Entry<String, ArrayList<String>> entry : requestFields.entrySet()) {
            String fieldName = entry.getKey();
            ListIterator<String> rulesIterator = entry.getValue().listIterator();

            while (rulesIterator.hasNext()) {
                testBuild(fieldName, rulesIterator);
            }
        }
    }

    private static void testBuild(String fieldName, ListIterator<String> iterador) {
        String[] rule = iterador.next().split(":");

        switch(rule[0])
        {
            case "required" :
                salida += "\n /** @test  */\n" +
                        "    public function the_"+ fieldName +"_field_is_required"+(isUpdatingTest?"_after_updating":"")+"()\n" +
                        "    {\n" +
                        "        $this->isRequiredField('"+ fieldName +"');\n" +
                        "    }";
                salida+= "\n";
                break;

            case "numeric" :
                salida += "\n /** @test  */\n" +
                        "    public function the_"+ fieldName +"_field_must_be_numeric"+(isUpdatingTest?"_after_updating":"")+"()\n" +
                        "    {\n" +
                        "       $this->isNumericField('"+ fieldName +"');\n" +
                        "    }";
                salida+= "\n";
                break;

            case "string" :
                salida += "\n /** @test  */\n" +
                        "    public function the_"+ fieldName +"_field_must_be_a_string"+(isUpdatingTest?"_after_updating":"")+"()\n" +
                        "    {\n" +
                        "        $this->assertFieldToFail('"+ fieldName +"', 'C4D3NA TEXT0');\n" +
                        "    }";
                salida+= "\n";
                break;

            case "boolean" :
                salida += "\n /** @test  */\n" +
                        "    public function the_"+ fieldName +"_must_a_boolean_field"+(isUpdatingTest?"_after_updating":"")+"()\n" +
                        "    {\n" +
                        "        $this->assertFieldToFail('"+ fieldName +"', 'True');\n" +
                        "    }";
                salida+= "\n";
                break;

            case "nullable" :
                salida += "\n /** @test  */\n" +
                        "    public function the_"+ fieldName +"_field_is_nullable"+(isUpdatingTest?"_after_updating":"")+"()\n" +
                        "    {\n" +
                        "    \t//ATENCION Tabla por defecto: "+databaseNames.get(0)+"\n" +
                        (!isUpdatingTest?"":"\t\t$"+modelNameSingular+" = "+modelNameSingular.substring(0, 1).toUpperCase() + modelNameSingular.substring(1)+"::factory()->create();\n")  +
                        "        $this->from(route('"+modelName+"."+(!isUpdatingTest?"create'":"edit', ['"+modelNameSingular+"' => $"+modelNameSingular+"]")+"))\n" +
                        "            ->"+(!isUpdatingTest?"post":"put")+"(route('"+modelName+"."+(!isUpdatingTest?"store'":"update', ['"+modelNameSingular+"' => $"+modelNameSingular+"]")+"), $this->withData([\n" +
                        "                '"+fieldName+"' => null\n" +
                        "            ]))->assertRedirect(route('"+modelName+"."+(!isUpdatingTest?"index'":"show', ['"+modelNameSingular+"' => $"+modelNameSingular+"]")+"));\n" +
                        "\n" +
                        "        $this->assertDatabaseHas('"+databaseNames.get(0)+"', [\n" +
                        "            '"+fieldName+"' => null,\n" +
                        "        ]);\n" +
                        "    }";
                salida+= "\n";
                break;

            case "regex" :
                salida += "\n /** @test  */\n" +
                        "    public function the_"+ fieldName +"_field_format_must_be_valid"+(isUpdatingTest?"_after_updating":"")+"()\n" +
                        "    {\n" +
                        "        $this->assertFieldToFail('"+ fieldName +"', 'InvalidField');\n" +
                        "    }";
                salida+= "\n";
                break;

            case "exists" :
                String[] param = rule[1].split(",");
                salida += "\n /** @test  */\n" +
                        "    public function the_"+ fieldName +"_field_must_exists_in_"+param[0]+"_table"+(isUpdatingTest?"_after_updating":"")+"()\n" +
                        "    {\n" +
                        "        //\n" +
                        "    }";
                salida+= "\n";
                break;

            case "array" :
                salida += "\n /** @test  */\n" +
                        "    public function the_"+ fieldName +"_field_must_be_an_array"+(isUpdatingTest?"_after_updating":"")+"()\n" +
                        "    {\n" +
                        "        $this->assertFieldToFail('"+ fieldName +"', '1,2,3');\n" +
                        "    }";
                salida+= "\n";
                break;

            case "min" :
                salida += "\n /** @test  */\n" +
                        "    public function the_"+ fieldName +"_minimal_length_must_be_"+rule[1]+(isUpdatingTest?"_after_updating":"")+"()\n" +
                        "    {\n" +
                        "         $this->assertFieldToFail('"+ fieldName +"',"+(Integer.parseInt(rule[1])-1)+");\n" +
                        "    }";
                salida+= "\n";
                break;

            case "max" :
                salida += "\n /** @test  */\n" +
                        "    public function the_"+ fieldName +"_maximun_length_must_be_"+rule[1]+(isUpdatingTest?"_after_updating":"")+"()\n" +
                        "    {\n" +
                        "         $this->assertFieldToFail('"+ fieldName +"',"+(Integer.parseInt(rule[1])+1)+");\n" +
                        "    }";
                salida+= "\n";
                break;
        }
    }

    private static void loadBaseTest(LinkedHashMap<String, String> equivalents){

        salida +="\n" +
                "  public function isNumericField($field): void\n" +
                "    {\n" +
                "        $this->assertFieldToFail($field, 'ABC');\n" +
                "    }\n" +
                "\n" +
                "    public function isRequiredField($field): void\n" +
                "    {\n" +
                "        $this->assertFieldToFail($field);\n" +
                "    }\n";

        if(!isUpdatingTest){

            salida += "\n"  +
                    "   /**\n" +
                    "     * @param $field\n" +
                    "     * @param $value\n" +
                    "     */\n" +
                    "    private function assertFieldToFail($field, $value=null): void\n" +
                    "    {\n" +
                    "        $this->handleValidationExceptions();\n" +
                    "\n" +
                    "        $this->from(route('"+modelName+".create'))\n" +
                    "            ->post(route('"+modelName+".store'), $this->withData([\n" +
                    "                $field => $value\n" +
                    "            ]))->assertSessionHasErrors([$field])\n" +
                    "            ->assertRedirect(url()->previous());\n" +
                    "\n";

                Iterator<String> iterator = databaseNames.iterator();
                while (iterator.hasNext()) {
                    salida += "\t\t$this->assertDatabaseEmpty('"+iterator.next()+"');\n";
                }

            salida += "}";

        }else{
            salida += "\n"  +
                    "   /**\n" +
                    "     * @param $field\n" +
                    "     * @param $value\n" +
                    "     */\n" +
                    "    private function assertFieldToFail($field, $value=null): void\n" +
                    "    {\n" +
                    "        $this->handleValidationExceptions();\n" +
                    "        $this->withExceptionHandling();\n" +
                    "\n" +
                    "       //ATENCION Modelos por defecto\n" +
                    "       $"+modelNameSingular+" = "+modelNameSingular.substring(0, 1).toUpperCase() + modelNameSingular.substring(1)+"::factory()->create();\n" +
                    "\n" +
                    "        $this->from(route('"+modelName+".edit', ['"+modelNameSingular+"' => $"+modelNameSingular+"]))\n" +
                    "            ->put(route('"+modelName+".update', ['"+modelNameSingular+"' => $"+modelNameSingular+"]), $this->withData([\n" +
                    "                $field => $value\n" +
                    "            ]))->assertSessionHasErrors([$field])\n" +
                    "            ->assertRedirect(url()->previous());\n" +
                    "\n";

                    Iterator<String> iterator = databaseNames.iterator();
                    while (iterator.hasNext()) {
                        String currentDBTable = iterator.next();
                        salida += "\t\t$this->assertDatabaseHas('"+currentDBTable+"', [\n";

                        for (Map.Entry<String, String> entry : equivalents.entrySet()) {
                          String tableFrom = entry.getValue();
                          if(tableFrom.equals(currentDBTable)){
                              salida += "\t\t\t'"+entry.getKey()+"' => $"+modelNameSingular+"->"+entry.getKey()+",\n";
                            }
                        }

                        salida += "\t\t]);\n" +
                                "\n";
                    }
                    salida += "}";
        }
    }

    private static void generateWebRoutes(){

        salida += "Route::get('/"+routeUriName+"', ["+controllerName+"::class, 'index'])->name('"+modelName+".index');\n" +
                "Route::get('/"+routeUriName+"/{"+modelNameSingular+"}/show', ["+controllerName+"::class, 'show'])->name('"+modelName+".show');\n" +
                "Route::get('/"+routeUriName+"/create', ["+controllerName+"::class, 'create'])->name('"+modelName+".create');\n" +
                "Route::post('/"+routeUriName+"/', ["+controllerName+"::class, 'store'])->name('"+modelName+".store');\n" +
                "Route::get('/"+routeUriName+"/{"+modelNameSingular+"}/edit', ["+controllerName+"::class, 'edit'])->name('"+modelName+".edit');\n" +
                "Route::put('/"+routeUriName+"/{"+modelNameSingular+"}', ["+controllerName+"::class, 'update'])->name('"+modelName+".update');\n" +
                "Route::delete('/"+routeUriName+"/{id}', ["+controllerName+"::class, 'destroy'])->name('"+modelName+".destroy');\n" +
                "\n";
    }

    private static void loadRequestRules(JSONParser parser, HashMap<String, ArrayList<String>> requestFields, LinkedHashMap<String, String> equivalents) {
        try {
            Object obj = parser.parse(new FileReader(Directory+"Request.json"));
            JSONObject requestModel = (JSONObject) obj;

            JSONArray fieldsList = (JSONArray) requestModel.get("Rules");
            JSONArray modelInfoList = (JSONArray) requestModel.get("Model");

            JSONObject modelInfoData = (JSONObject) modelInfoList.get(0);
            modelName = modelInfoData.get("modelName").toString();
            modelNameSingular = modelInfoData.get("modelNameSingular").toString();
            databaseNames = (JSONArray) modelInfoData.get("databaseNames");
            controllerName = modelInfoData.get("controllerName").toString();
            routeUriName = modelInfoData.get("routeUriName").toString();

            for (int i = 0; i < fieldsList.size(); i++){
                JSONObject currentField = (JSONObject) fieldsList.get(i);
                ArrayList<String> validations = new ArrayList<>();
                JSONArray validationTypes = (JSONArray) currentField.get("validations");

                Iterator<String> iterator = validationTypes.iterator();
                while (iterator.hasNext()) {
                    validations.add(iterator.next());
                }
                requestFields.put(currentField.get("field").toString(), validations);
                equivalents.put(currentField.get("field").toString(), currentField.get("from").toString());
            }
        }
        catch (FileNotFoundException e){}
        catch (IOException e){}
        catch (ParseException e){}
    }
}
