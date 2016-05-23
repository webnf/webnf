(ns webnf.mui.component
  #?@
  (:clj
   [(:refer-clojure :exclude [list])
    (:require
     [clojure.tools.logging :as log]
     [webnf.base.cljc :refer [defmacro*]]
     [om-tools.dom :as dom])]
   :cljs
   [(:refer-clojure :exclude [list])
    (:require-macros [webnf.mui.base :refer [defcomponent component]]
                     [webnf.mui.component :refer [gen-wrappers]])
    (:require
     webnf.mui.bundle
     [webnf.base.logging :as log :include-macros true]
     [webnf.mui.base :refer [add-styles]]
     [webnf.mui.style :refer [style-class]]
     [webnf.mui.theme :refer [theme-sheet]]
     [om.next :as om]
     [om-tools.dom :as dom]
     [clojure.string :as str]
     [garden.units :refer [px s percent]]
     [garden.compiler :refer [render-css]])]))

#?
(:clj
 (do
   (def widgets
     '{app-bar AppBar
       auto-complete AutoComplete
       avatar Avatar
       badge Badge
       card Card
       card-actions CardActions
       card-header CardHeader
       card-media CardMedia
       card-title CardTitle
       card-text CardText
       checkbox Checkbox
       circular-progress CircularProgress
       date-picker DatePicker
       dialog Dialog
       divider Divider
       drawer Drawer
       drop-down-menu DropDownMenu
       flat-button FlatButton
       floating-action-button FloatingActionButton
       font-icon FontIcon
       grid-list GridList
       grid-tile GridTile
       icon-button IconButton
       icon-menu IconMenu
       linear-progress LinearProgress
       list List
       list-item ListItem
       make-selectable MakeSelectable
       menu Menu
       menu-item MenuItem
       paper Paper
       popover Popover
       radio-button RadioButton
       radio-button-group RadioButtonGroup
       raised-button RaisedButton
       refresh-indicator RefreshIndicator
       select-field SelectField
       slider Slider
       subheader Subheader
       svg-icon SvgIcon
       step Step
       step-button StepButton
       step-content StepContent
       step-label StepLabel
       stepper Stepper
       snackbar Snackbar
       tabs Tabs
       tab Tab
       table Table
       table-body TableBody
       table-footer TableFooter
       table-header TableHeader
       table-header-column TableHeaderColumn
       table-row TableRow
       table-row-column TableRowColumn
       text-field TextField
       time-picker TimePicker
       toggle Toggle
       toolbar Toolbar
       toolbar-group ToolbarGroup
       toolbar-separator ToolbarSeparator
       toolbar-title ToolbarTitle})

   (defn- wrapper-body [cls opts children]
     (if (dom/literal? opts)
       (let [[opts children] (dom/element-args opts children)]
         (if (every? (complement dom/possible-coll?) children)
           `(create-element ~cls ~opts ~(vec children))
           `(create-element ~cls ~opts (flatten ~(vec children)))))
       `(let [[opts# children#] (dom/element-args ~opts ~(vec children))]
          (create-element ~cls opts# (flatten children#)))))
   
   (defn gen-wrapper-macro [[fname cname]]
     `(defmacro ~fname [& [opts# & children#]]
        {:arglists '([opts? & children])}
        (wrapper-body '~(symbol (str "js/ReactMUI." cname)) opts# children#)))

   (defn gen-wrapper-fn [[fname cname]]
     (let [cname (symbol (str "js/ReactMUI." cname))]
       `(defn ~fname
          ([]
           (create-element ~cname nil nil))
          ([opts# & children#]
           (let [[opts# children#] (dom/element-args opts# children#)]
             (create-element ~cname opts# (flatten children#)))))))
   
   (defmacro* ^:private gen-wrappers []
     :clj  `(do ~@(map gen-wrapper-macro widgets))
     :cljs `(do ~@(map gen-wrapper-fn widgets))))
 :cljs
 (defn create-element [cls opts children]
   (js/React.createElement
    cls opts
    (case (count children)
      0 nil
      1 (first children)
      (into-array children)))))

(gen-wrappers)
