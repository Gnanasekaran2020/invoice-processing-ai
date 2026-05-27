import {
  NzOutletModule,
  NzStringTemplateOutletDirective
} from "./chunk-XEJJKW5U.js";
import {
  ChangeDetectionStrategy,
  Component,
  Input,
  NgModule,
  ViewEncapsulation$1,
  booleanAttribute,
  setClassMetadata,
  ɵɵInputTransformsFeature,
  ɵɵStandaloneFeature,
  ɵɵadvance,
  ɵɵclassProp,
  ɵɵconditional,
  ɵɵdefineComponent,
  ɵɵdefineInjector,
  ɵɵdefineNgModule,
  ɵɵelementContainerEnd,
  ɵɵelementContainerStart,
  ɵɵelementEnd,
  ɵɵelementStart,
  ɵɵnextContext,
  ɵɵproperty,
  ɵɵtemplate,
  ɵɵtext,
  ɵɵtextInterpolate
} from "./chunk-L7PJK5KF.js";
import "./chunk-ISM5WLAM.js";
import "./chunk-IC62NIWK.js";
import "./chunk-ZZ67MR3E.js";
import "./chunk-WDMUDEB6.js";

// node_modules/ng-zorro-antd/fesm2022/ng-zorro-antd-divider.mjs
function NzDividerComponent_Conditional_0_ng_container_1_Template(rf, ctx) {
  if (rf & 1) {
    ɵɵelementContainerStart(0);
    ɵɵtext(1);
    ɵɵelementContainerEnd();
  }
  if (rf & 2) {
    const ctx_r0 = ɵɵnextContext(2);
    ɵɵadvance();
    ɵɵtextInterpolate(ctx_r0.nzText);
  }
}
function NzDividerComponent_Conditional_0_Template(rf, ctx) {
  if (rf & 1) {
    ɵɵelementStart(0, "span", 0);
    ɵɵtemplate(1, NzDividerComponent_Conditional_0_ng_container_1_Template, 2, 1, "ng-container", 1);
    ɵɵelementEnd();
  }
  if (rf & 2) {
    const ctx_r0 = ɵɵnextContext();
    ɵɵadvance();
    ɵɵproperty("nzStringTemplateOutlet", ctx_r0.nzText);
  }
}
var NzDividerComponent = class _NzDividerComponent {
  constructor() {
    this.nzType = "horizontal";
    this.nzOrientation = "center";
    this.nzDashed = false;
    this.nzPlain = false;
  }
  static {
    this.ɵfac = function NzDividerComponent_Factory(__ngFactoryType__) {
      return new (__ngFactoryType__ || _NzDividerComponent)();
    };
  }
  static {
    this.ɵcmp = ɵɵdefineComponent({
      type: _NzDividerComponent,
      selectors: [["nz-divider"]],
      hostAttrs: [1, "ant-divider"],
      hostVars: 16,
      hostBindings: function NzDividerComponent_HostBindings(rf, ctx) {
        if (rf & 2) {
          ɵɵclassProp("ant-divider-horizontal", ctx.nzType === "horizontal")("ant-divider-vertical", ctx.nzType === "vertical")("ant-divider-with-text", ctx.nzText)("ant-divider-plain", ctx.nzPlain)("ant-divider-with-text-left", ctx.nzText && ctx.nzOrientation === "left")("ant-divider-with-text-right", ctx.nzText && ctx.nzOrientation === "right")("ant-divider-with-text-center", ctx.nzText && ctx.nzOrientation === "center")("ant-divider-dashed", ctx.nzDashed);
        }
      },
      inputs: {
        nzText: "nzText",
        nzType: "nzType",
        nzOrientation: "nzOrientation",
        nzDashed: [2, "nzDashed", "nzDashed", booleanAttribute],
        nzPlain: [2, "nzPlain", "nzPlain", booleanAttribute]
      },
      exportAs: ["nzDivider"],
      standalone: true,
      features: [ɵɵInputTransformsFeature, ɵɵStandaloneFeature],
      decls: 1,
      vars: 1,
      consts: [[1, "ant-divider-inner-text"], [4, "nzStringTemplateOutlet"]],
      template: function NzDividerComponent_Template(rf, ctx) {
        if (rf & 1) {
          ɵɵtemplate(0, NzDividerComponent_Conditional_0_Template, 2, 1, "span", 0);
        }
        if (rf & 2) {
          ɵɵconditional(ctx.nzText ? 0 : -1);
        }
      },
      dependencies: [NzOutletModule, NzStringTemplateOutletDirective],
      encapsulation: 2,
      changeDetection: 0
    });
  }
};
(() => {
  (typeof ngDevMode === "undefined" || ngDevMode) && setClassMetadata(NzDividerComponent, [{
    type: Component,
    args: [{
      selector: "nz-divider",
      exportAs: "nzDivider",
      preserveWhitespaces: false,
      encapsulation: ViewEncapsulation$1.None,
      changeDetection: ChangeDetectionStrategy.OnPush,
      template: `
    @if (nzText) {
      <span class="ant-divider-inner-text">
        <ng-container *nzStringTemplateOutlet="nzText">{{ nzText }}</ng-container>
      </span>
    }
  `,
      host: {
        class: "ant-divider",
        "[class.ant-divider-horizontal]": `nzType === 'horizontal'`,
        "[class.ant-divider-vertical]": `nzType === 'vertical'`,
        "[class.ant-divider-with-text]": `nzText`,
        "[class.ant-divider-plain]": `nzPlain`,
        "[class.ant-divider-with-text-left]": `nzText && nzOrientation === 'left'`,
        "[class.ant-divider-with-text-right]": `nzText && nzOrientation === 'right'`,
        "[class.ant-divider-with-text-center]": `nzText && nzOrientation === 'center'`,
        "[class.ant-divider-dashed]": `nzDashed`
      },
      imports: [NzOutletModule],
      standalone: true
    }]
  }], () => [], {
    nzText: [{
      type: Input
    }],
    nzType: [{
      type: Input
    }],
    nzOrientation: [{
      type: Input
    }],
    nzDashed: [{
      type: Input,
      args: [{
        transform: booleanAttribute
      }]
    }],
    nzPlain: [{
      type: Input,
      args: [{
        transform: booleanAttribute
      }]
    }]
  });
})();
var NzDividerModule = class _NzDividerModule {
  static {
    this.ɵfac = function NzDividerModule_Factory(__ngFactoryType__) {
      return new (__ngFactoryType__ || _NzDividerModule)();
    };
  }
  static {
    this.ɵmod = ɵɵdefineNgModule({
      type: _NzDividerModule,
      imports: [NzDividerComponent],
      exports: [NzDividerComponent]
    });
  }
  static {
    this.ɵinj = ɵɵdefineInjector({
      imports: [NzDividerComponent]
    });
  }
};
(() => {
  (typeof ngDevMode === "undefined" || ngDevMode) && setClassMetadata(NzDividerModule, [{
    type: NgModule,
    args: [{
      imports: [NzDividerComponent],
      exports: [NzDividerComponent]
    }]
  }], null, null);
})();
export {
  NzDividerComponent,
  NzDividerModule
};
//# sourceMappingURL=ng-zorro-antd_divider.js.map
