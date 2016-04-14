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
     '{nav-menu-icon Icons.NavigationMenu
       nav-left-icon Icons.NavigationChevronLeft
       nav-right-icon Icons.NavigationChevronRight
       app-bar AppBar
       app-canvas AppCanvas
       auto-complete AutoComplete
       avatar Avatar
       badge Badge
       before-after-wrapper BeforeAfterWrapper
       card Card
       card-actions CardActions
       card-expandable CardExpandable
       card-header CardHeader
       card-media CardMedia
       card-text CardText
       card-title CardTitle
       checkbox Checkbox
       circular-progress CircularProgress
       clear-fix ClearFix
       date-picker DatePicker
       date-picker-dialog DatePickerDialog
       dialog Dialog
       divider Divider
       drop-down-icon DropDownIcon
       drop-down-menu DropDownMenu
       enhanced-button EnhancedButton
       flat-button FlatButton
       floating-action-button FloatingActionButton
       font-icon FontIcon
       grid-list GridList
       grid-tile GridTile
       icon-button IconButton
       icon-menu IconMenu
       left-nav LeftNav
       linear-progress LinearProgress
       list List
       list-divider ListDivider
       list-item ListItem
       menu Menu
       menu-item MenuItem
       mixins Mixins
       overlay Overlay
       paper Paper
       pop-over Popover
       radio-button RadioButton
       radio-button-group RadioButtonGroup
       raised-button RaisedButton
       refresh-indicator RefreshIndicator
       ripples Ripples
       select-field SelectField
       selectable-container-enhance SelectableContainerEnhance
       slider Slider
       svg-icon SvgIcon
       styles Styles
       snackbar Snackbar
       tab Tab
       tabs Tabs
       table Table
       table-body TableBody
       table-footer TableFooter
       table-header TableHeader
       table-header-column TableHeaderColumn
       table-row TableRow
       table-row-column TableRowColumn
       toggle Toggle
       theme-wrapper ThemeWrapper
       time-picker TimePicker
       text-field TextField
       toolbar Toolbar
       toolbar-group ToolbarGroup
       toolbar-separator ToolbarSeparator
       toolbartitle ToolbarTitle
       tooltip Tooltip
       utils Utils})

   (defn- wrapper-body [cls opts children]
     (if (dom/literal? opts)
       (let [[opts children] (dom/element-args opts children)]
         (log/trace "PRE" cls opts children)
         (if (every? (complement dom/possible-coll?) children)
           `(do
              (webnf.base.logging/trace "B1" ~(str cls) ~(pr-str opts) ~(pr-str children))
              (create-element ~cls ~opts ~(vec children)))
           `(do
              (webnf.base.logging/trace "B2" ~(str cls) ~(pr-str opts) ~(pr-str (flatten (vec children))))
              (create-element ~cls ~opts (flatten ~(vec children))))))
       (do (log/trace "RT" cls opts children)
           `(let [[opts# children#] (dom/element-args ~opts ~(vec children))]
              (webnf.base.logging/trace "B3" ~(str cls) opts# children#)
              (create-element ~cls opts# (flatten children#))))))
   
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
   (js/React.createElement cls opts (into-array children))))

(gen-wrappers)
