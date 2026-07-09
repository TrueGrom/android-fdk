plugins {
    `java-platform`
    alias(libs.plugins.fdk.bom.publish)
}

dependencies {
    constraints {
        api(project(":utils"))
        api(project(":state"))
        api(project(":repository"))
        api(project(":viewmodel"))
        api(project(":logging"))
        api(project(":http-error"))
        api(project(":crypto"))
        api(project(":network"))
        api(project(":datetime"))
        api(project(":ui-kit"))
        api(project(":screen"))
    }
}
